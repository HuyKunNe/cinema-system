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
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.FareMatrixCell;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.entity.FareMatrixScope;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;
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

    private FareMatrixResponse loadMatrix(UUID cinemaId, UUID policyId, UUID versionId) {

        requireCinema(cinemaId);
        requirePolicy(policyId);

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

        Definition definition = normalize(request);

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
        FareMatrixRules.validateUniformFares(revision.getDayGroup(), definition.values());

        Map<Key, Value> current = readValues(versionId);

        boolean unchanged =
                revision.getFareScopes().equals(definition.scopes())
                        && revision.getFareTicketCategories().equals(definition.categories())
                        && revision.getFareSeatTypes().equals(definition.seatTypes())
                        && current.equals(definition.values());

        if (unchanged) {
            return render(revision, current);
        }

        // Remove cells before replacing the scope rows referenced by their FKs.
        cellRepository.deleteForVersion(versionId);

        revision.replaceDraftMatrixDefinition(
                definition.scopes(), definition.categories(), definition.seatTypes());

        // Persist collection rows before inserting cells that reference them.
        versionRepository.flush();

        List<FareMatrixCell> replacements =
                definition.values().values().stream()
                        .map(value -> new FareMatrixCell(revision, value))
                        .toList();

        cellRepository.saveAll(replacements);
        cellRepository.flush();

        return render(revision, definition.values());
    }

    private Definition normalize(ReplaceFareMatrixRequest request) {
        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || request.scopes() == null
                || request.scopes().size() > 10
                || request.cells() == null
                || request.cells().size() > 320) {
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

        Set<TicketCategory> categories =
                uniqueEnums(request.ticketCategories(), TicketCategory.class);

        Set<SeatType> seatTypes = uniqueEnums(request.seatTypes(), SeatType.class);

        if (scopes.isEmpty()) {
            if (!categories.isEmpty() || !seatTypes.isEmpty() || !request.cells().isEmpty()) {
                throw invalid();
            }
        } else if (!categories.contains(TicketCategory.ADULT) || seatTypes.isEmpty()) {
            throw invalid();
        }

        Set<Key> expected = expectedKeys(scopes, categories, seatTypes);
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
                            input.timeBand(),
                            input.ticketCategory(),
                            input.seatType());

            if (!expected.contains(key) || !seen.add(key)) {
                throw invalid();
            }

            Value value = new Value(key, input.state(), input.amount());

            if (value.state() != FareCellState.UNCONFIGURED) {
                values.put(key, value);
            }
        }

        return new Definition(scopes, categories, seatTypes, values);
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

        Set<FareMatrixScope> scopes = revision.getFareScopes();
        Set<TicketCategory> categories = revision.getFareTicketCategories();
        Set<SeatType> seatTypes = revision.getFareSeatTypes();

        Set<Key> expected = expectedKeys(scopes, categories, seatTypes);

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
                            key.timeBand(),
                            key.ticketCategory(),
                            key.seatType(),
                            value == null ? FareCellState.UNCONFIGURED : value.state(),
                            value == null ? null : value.amount(),
                            unit(key.seatType())));
        }

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
                categories.stream().sorted().toList(),
                seatTypes.stream().sorted().toList(),
                required,
                configured,
                missing,
                required > 0 && missing == 0,
                List.copyOf(cells),
                options());
    }

    private Set<Key> expectedKeys(
            Set<FareMatrixScope> scopes, Set<TicketCategory> categories, Set<SeatType> seatTypes) {

        Set<Key> keys = new LinkedHashSet<>();

        for (FareMatrixScope scope : sortedScopes(scopes)) {
            for (PricingTimeBand timeBand : PricingTimeBand.values()) {
                for (TicketCategory category : categories.stream().sorted().toList()) {
                    for (SeatType seatType : seatTypes.stream().sorted().toList()) {

                        keys.add(
                                new Key(
                                        scope.getExperienceType(),
                                        scope.getContentFormat(),
                                        timeBand,
                                        category,
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
                Arrays.asList(PricingTimeBand.values()),
                Arrays.asList(TicketCategory.values()),
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

    private void requirePolicy(UUID policyId) {
        if (!policyRepository.existsById(policyId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND);
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }

    private record Definition(
            Set<FareMatrixScope> scopes,
            Set<TicketCategory> categories,
            Set<SeatType> seatTypes,
            Map<Key, Value> values) {}
}
