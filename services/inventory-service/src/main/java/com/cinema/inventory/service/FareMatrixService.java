package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceFareMatrixRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.Options;
import com.cinema.inventory.dto.response.FareMatrixResponse.ScopeResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.SeatTypeOption;
import com.cinema.inventory.dto.response.FareMatrixResponse.TicketGroupResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.TimeBandResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.FareMatrixCell;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.entity.FareMatrixScope;
import com.cinema.inventory.entity.PricingTicketGroup;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.FareMatrixCellRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class FareMatrixService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final FareMatrixCellRepository cellRepository;

    public FareMatrixService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            FareMatrixCellRepository cellRepository) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.cellRepository = cellRepository;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public FareMatrixResponse get(UUID cinemaId, UUID policyId, UUID versionId) {

        return loadMatrix(cinemaId, policyId, versionId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FareMatrixResponse getForPublication(UUID cinemaId, UUID policyId, UUID versionId) {

        return loadMatrix(cinemaId, policyId, versionId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FareMatrixResponse getForShowtimePricing(UUID cinemaId, UUID policyId, UUID versionId) {

        return loadMatrix(cinemaId, policyId, versionId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public FareMatrixResponse getForConfiguration(UUID cinemaId, UUID policyId, UUID versionId) {

        return loadMatrix(cinemaId, policyId, versionId);
    }

    private FareMatrixResponse loadMatrix(UUID cinemaId, UUID policyId, UUID versionId) {

        requireCinema(cinemaId);

        if (!policyRepository.existsById(policyId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND);
        }

        CinemaPricingPolicyVersion revision =
                versionRepository
                        .findForRead(cinemaId, policyId, versionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_VERSION_NOT_FOUND));

        return render(revision, readValues(versionId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FareMatrixResponse replace(
            UUID cinemaId, UUID policyId, UUID versionId, ReplaceFareMatrixRequest request) {

        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {
            throw invalid();
        }

        requireCinema(cinemaId);

        policyRepository
                .findByIdForUpdate(policyId)
                .orElseThrow(
                        () -> new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        CinemaPricingPolicyVersion revision =
                versionRepository
                        .findForUpdate(policyId, versionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_VERSION_NOT_FOUND));

        if (!revision.appliesTo(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_FOUND);
        }

        if (!request.expectedVersion().equals(revision.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        revision.requireDraft();
        revision.validatePricingDefinitions();

        Definition definition = normalize(request, revision);

        FareMatrixRules.validateUniformFares(revision.getDayGroup(), definition.values());

        Map<Key, Value> current = readValues(versionId);

        boolean unchanged =
                revision.getFareScopes().equals(definition.scopes())
                        && revision.getFareSeatTypes().equals(definition.seatTypes())
                        && current.equals(definition.values());

        if (unchanged) {
            return render(revision, current);
        }

        cellRepository.deleteForVersion(versionId);

        revision.replaceDraftMatrixDefinition(definition.scopes(), definition.seatTypes());

        versionRepository.flush();

        List<FareMatrixCell> replacements =
                definition.values().values().stream()
                        .map(value -> new FareMatrixCell(revision, value))
                        .toList();

        cellRepository.saveAll(replacements);
        cellRepository.flush();

        return render(revision, definition.values());
    }

    private Definition normalize(
            ReplaceFareMatrixRequest request, CinemaPricingPolicyVersion revision) {

        if (request.scopes() == null || request.scopes().size() > 10 || request.cells() == null) {
            throw invalid();
        }

        Set<FareMatrixScope> scopes = new HashSet<>();

        for (var input : request.scopes()) {
            if (input == null
                    || !scopes.add(
                            new FareMatrixScope(input.experienceType(), input.contentFormat()))) {
                throw invalid();
            }
        }

        Set<SeatType> seatTypes = uniqueEnums(request.seatTypes(), SeatType.class);

        if (scopes.isEmpty()) {
            if (!seatTypes.isEmpty() || !request.cells().isEmpty()) {
                throw invalid();
            }
        } else if (seatTypes.isEmpty()) {
            throw invalid();
        }

        Set<Key> expected = expectedKeys(revision, scopes, seatTypes);

        if (request.cells().size() > expected.size()) {
            throw invalid();
        }

        Set<Key> seen = new HashSet<>();
        Map<Key, Value> values = new HashMap<>();

        for (var input : request.cells()) {
            if (input == null) {
                throw invalid();
            }

            Key key =
                    new Key(
                            input.experienceType(),
                            input.contentFormat(),
                            input.timeBandId(),
                            input.ticketGroupId(),
                            input.seatType());

            if (!expected.contains(key) || !seen.add(key)) {
                throw invalid();
            }

            Value value = new Value(key, input.state(), input.amount());

            if (value.state() != FareCellState.UNCONFIGURED) {
                values.put(key, value);
            }
        }

        return new Definition(scopes, seatTypes, values);
    }

    private Map<Key, Value> readValues(UUID versionId) {
        Map<Key, Value> values = new HashMap<>();

        for (FareMatrixCell cell : cellRepository.findAllByPolicyVersion_Id(versionId)) {

            Value value;

            try {
                value = cell.toValue();
            } catch (ValidationException exception) {
                throw inconsistent();
            }

            if (value.state() == FareCellState.UNCONFIGURED
                    || values.putIfAbsent(value.key(), value) != null) {
                throw inconsistent();
            }
        }

        return values;
    }

    private FareMatrixResponse render(CinemaPricingPolicyVersion revision, Map<Key, Value> values) {

        try {
            revision.validatePricingDefinitions();
        } catch (ValidationException exception) {
            throw inconsistent();
        }

        Set<FareMatrixScope> scopes = revision.getFareScopes();
        Set<SeatType> seatTypes = revision.getFareSeatTypes();

        Set<Key> expected = expectedKeys(revision, scopes, seatTypes);

        if (!expected.containsAll(values.keySet())) {
            throw inconsistent();
        }

        List<CellResponse> cells = new ArrayList<>();

        for (Key key : expected) {
            Value value = values.get(key);

            cells.add(
                    new CellResponse(
                            key.experienceType(),
                            key.contentFormat(),
                            key.timeBandId(),
                            key.ticketGroupId(),
                            key.seatType(),
                            value == null ? FareCellState.UNCONFIGURED : value.state(),
                            value == null ? null : value.amount(),
                            unit(key.seatType())));
        }

        List<TimeBandResponse> timeBands =
                revision.getPricingTimeBands().stream()
                        .map(
                                item ->
                                        new TimeBandResponse(
                                                item.getId(),
                                                item.getName(),
                                                item.getStartMinute(),
                                                item.getEndMinute(),
                                                item.getSortOrder()))
                        .toList();

        List<TicketGroupResponse> ticketGroups =
                revision.getPricingTicketGroups().stream()
                        .map(
                                item ->
                                        new TicketGroupResponse(
                                                item.getId(),
                                                item.getName(),
                                                item.getEligibilityDescription(),
                                                item.isDefaultGroup(),
                                                item.getSortOrder()))
                        .toList();

        UUID defaultTicketGroupId =
                revision.getPricingTicketGroups().stream()
                        .filter(PricingTicketGroup::isDefaultGroup)
                        .findFirst()
                        .orElseThrow(FareMatrixService::inconsistent)
                        .getId();

        int required = expected.size();
        int configured = values.size();
        int missing = required - configured;

        return new FareMatrixResponse(
                revision.getPolicy().getId(),
                revision.getId(),
                revision.getRevision(),
                revision.getVersion(),
                revision.getDayGroup(),
                revision.getStatus(),
                sortedScopes(scopes).stream()
                        .map(
                                scope ->
                                        new ScopeResponse(
                                                scope.getExperienceType(),
                                                scope.getContentFormat()))
                        .toList(),
                timeBands,
                ticketGroups,
                defaultTicketGroupId,
                seatTypes.stream().sorted().toList(),
                required,
                configured,
                missing,
                required > 0 && missing == 0,
                List.copyOf(cells),
                options());
    }

    private Set<Key> expectedKeys(
            CinemaPricingPolicyVersion revision,
            Set<FareMatrixScope> scopes,
            Set<SeatType> seatTypes) {

        var timeBands = revision.getPricingTimeBands();
        var ticketGroups = revision.getPricingTicketGroups();

        long count = scopes.size();

        for (int size : new int[] {timeBands.size(), ticketGroups.size(), seatTypes.size()}) {

            count *= size;

            if (count > Integer.MAX_VALUE) {
                throw invalid();
            }
        }

        Set<Key> keys = new LinkedHashSet<>();

        for (FareMatrixScope scope : sortedScopes(scopes)) {
            for (var timeBand : timeBands) {
                for (var ticketGroup : ticketGroups) {
                    for (SeatType seatType : seatTypes.stream().sorted().toList()) {

                        keys.add(
                                new Key(
                                        scope.getExperienceType(),
                                        scope.getContentFormat(),
                                        timeBand.getId(),
                                        ticketGroup.getId(),
                                        seatType));
                    }
                }
            }
        }

        return keys;
    }

    private List<FareMatrixScope> sortedScopes(Set<FareMatrixScope> scopes) {

        return scopes.stream()
                .sorted(
                        Comparator.comparing(FareMatrixScope::getExperienceType)
                                .thenComparing(FareMatrixScope::getContentFormat))
                .toList();
    }

    private Options options() {
        return new Options(
                Arrays.asList(PricingExperienceType.values()),
                Arrays.asList(ContentFormat.values()),
                Arrays.stream(SeatType.values())
                        .map(type -> new SeatTypeOption(type, unit(type), type.getCapacity()))
                        .toList(),
                Arrays.asList(FareCellState.values()));
    }

    private FareUnit unit(SeatType seatType) {
        return seatType == SeatType.COUPLE ? FareUnit.PAIR : FareUnit.SEAT;
    }

    private <E extends Enum<E>> Set<E> uniqueEnums(List<E> values, Class<E> type) {

        if (values == null || values.size() > type.getEnumConstants().length) {
            throw invalid();
        }

        Set<E> result = EnumSet.noneOf(type);

        for (E value : values) {
            if (value == null || !result.add(value)) {
                throw invalid();
            }
        }

        return result;
    }

    private void requireCinema(UUID cinemaId) {
        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }

    private record Definition(
            Set<FareMatrixScope> scopes, Set<SeatType> seatTypes, Map<Key, Value> values) {}
}
