package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreatePricingPolicyDraftRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.FareMatrixCell;
import com.cinema.inventory.entity.FareMatrixCell.Key;
import com.cinema.inventory.entity.FareMatrixCell.Value;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.FareMatrixCellRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PricingPolicyDraftService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final FareMatrixCellRepository cellRepository;
    private final FareMatrixService fareMatrixService;
    private final CinemaPricingPolicyMapper mapper;

    public PricingPolicyDraftService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            FareMatrixCellRepository cellRepository,
            FareMatrixService fareMatrixService,
            CinemaPricingPolicyMapper mapper) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.cellRepository = cellRepository;
        this.fareMatrixService = fareMatrixService;
        this.mapper = mapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreateResult create(
            UUID cinemaId,
            UUID policyId,
            UUID sourceVersionId,
            CreatePricingPolicyDraftRequest request) {

        if (request == null || request.clientRequestId() == null) {
            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
        }

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }

        policyRepository
                .findByIdForUpdate(policyId)
                .orElseThrow(
                        () -> new NotFoundException(InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        CinemaPricingPolicyVersion source =
                versionRepository
                        .findForUpdate(policyId, sourceVersionId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode
                                                        .PRICING_POLICY_VERSION_NOT_FOUND));

        if (!source.appliesTo(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_FOUND);
        }

        var existing =
                versionRepository.findByPolicy_IdAndDraftClientRequestId(
                        policyId, request.clientRequestId());

        if (existing.isPresent()) {
            CinemaPricingPolicyVersion revision = existing.get();

            if (!sourceVersionId.equals(revision.getDraftSourceVersionId())) {

                throw new ConflictException(
                        InventoryErrorCode.PRICING_POLICY_DRAFT_CREATE_CONFLICT);
            }

            return new CreateResult(false, mapper.toResponse(revision));
        }

        if (source.getStatus() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_SOURCE_NOT_PUBLISHED);
        }

        var matrix = fareMatrixService.getForPublication(cinemaId, policyId, sourceVersionId);

        if (!matrix.coverageComplete()) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
        }

        long highestRevision = versionRepository.findHighestRevision(policyId);

        if (highestRevision == Long.MAX_VALUE) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_REVISION_EXHAUSTED);
        }

        CinemaPricingPolicyVersion draft =
                versionRepository.saveAndFlush(
                        source.newDraft(highestRevision + 1, request.clientRequestId()));

        List<FareMatrixCell> copiedCells =
                matrix.cells().stream()
                        .map(
                                cell ->
                                        new FareMatrixCell(
                                                draft,
                                                new Value(
                                                        new Key(
                                                                cell.experienceType(),
                                                                cell.contentFormat(),
                                                                cell.timeBand(),
                                                                cell.ticketCategory(),
                                                                cell.seatType()),
                                                        cell.state(),
                                                        cell.amount())))
                        .toList();

        cellRepository.saveAll(copiedCells);
        cellRepository.flush();

        return new CreateResult(true, mapper.toResponse(draft));
    }

    public record CreateResult(boolean created, CinemaPricingPolicyVersionResponse response) {}
}
