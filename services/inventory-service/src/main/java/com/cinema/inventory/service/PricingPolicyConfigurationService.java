package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplacePricingConfigurationRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion.TicketGroupEdit;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion.TimeBandEdit;
import com.cinema.inventory.entity.PricingTicketGroup;
import com.cinema.inventory.entity.PricingTimeBandDefinition;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.FareMatrixCellRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PricingPolicyConfigurationService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final FareMatrixCellRepository cellRepository;
    private final FareMatrixService fareMatrixService;

    public PricingPolicyConfigurationService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            FareMatrixCellRepository cellRepository,
            FareMatrixService fareMatrixService) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.cellRepository = cellRepository;
        this.fareMatrixService = fareMatrixService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public FareMatrixResponse replace(
            UUID cinemaId,
            UUID policyId,
            UUID versionId,
            ReplacePricingConfigurationRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || request.timeBands() == null
                || request.timeBands().isEmpty()
                || request.ticketGroups() == null
                || request.ticketGroups().isEmpty()) {

            throw invalid();
        }

        List<TimeBandEdit> timeBands = toTimeBands(request);
        List<TicketGroupEdit> ticketGroups = toTicketGroups(request);

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }

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

        // Validate before deleting cells or changing managed entities.
        revision.validatePricingDefinitionEdits(timeBands, ticketGroups);

        Set<UUID> retainedTimeBandIds =
                timeBands.stream()
                        .map(TimeBandEdit::id)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Set<UUID> retainedTicketGroupIds =
                ticketGroups.stream()
                        .map(TicketGroupEdit::id)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Set<UUID> removedTimeBandIds =
                revision.getPricingTimeBands().stream()
                        .map(PricingTimeBandDefinition::getId)
                        .filter(id -> !retainedTimeBandIds.contains(id))
                        .collect(Collectors.toSet());

        Set<UUID> removedTicketGroupIds =
                revision.getPricingTicketGroups().stream()
                        .map(PricingTicketGroup::getId)
                        .filter(id -> !retainedTicketGroupIds.contains(id))
                        .collect(Collectors.toSet());

        // Delete referencing rows before orphan removal can delete definitions.
        if (!removedTimeBandIds.isEmpty()) {
            cellRepository.deleteForTimeBands(versionId, removedTimeBandIds);
        }

        if (!removedTicketGroupIds.isEmpty()) {
            cellRepository.deleteForTicketGroups(versionId, removedTicketGroupIds);
        }

        revision.replaceDraftPricingDefinitions(timeBands, ticketGroups);

        versionRepository.flush();

        return fareMatrixService.getForConfiguration(cinemaId, policyId, versionId);
    }

    private static List<TimeBandEdit> toTimeBands(ReplacePricingConfigurationRequest request) {

        return request.timeBands().stream()
                .map(
                        input -> {
                            if (input == null
                                    || input.startMinute() == null
                                    || input.endMinute() == null
                                    || input.sortOrder() == null) {

                                throw invalid();
                            }

                            return new TimeBandEdit(
                                    input.id(),
                                    new PricingTimeBandDefinition.Details(
                                            input.name(),
                                            input.startMinute(),
                                            input.endMinute(),
                                            input.sortOrder()));
                        })
                .toList();
    }

    private static List<TicketGroupEdit> toTicketGroups(
            ReplacePricingConfigurationRequest request) {

        return request.ticketGroups().stream()
                .map(
                        input -> {
                            if (input == null
                                    || input.defaultGroup() == null
                                    || input.sortOrder() == null) {

                                throw invalid();
                            }

                            return new TicketGroupEdit(
                                    input.id(),
                                    new PricingTicketGroup.Details(
                                            input.name(),
                                            input.eligibilityDescription(),
                                            input.defaultGroup(),
                                            input.sortOrder()));
                        })
                .toList();
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_PRICING_DEFINITION);
    }
}
