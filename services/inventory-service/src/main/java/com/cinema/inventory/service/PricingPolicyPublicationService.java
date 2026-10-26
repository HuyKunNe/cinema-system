package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.PublishPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class PricingPolicyPublicationService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final FareMatrixService fareMatrixService;
    private final DraftFareMatrixValidationService validationService;
    private final CinemaPricingPolicyMapper mapper;
    private final Clock clock;

    public PricingPolicyPublicationService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            FareMatrixService fareMatrixService,
            DraftFareMatrixValidationService validationService,
            CinemaPricingPolicyMapper mapper,
            Clock clock) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.versionRepository = versionRepository;
        this.fareMatrixService = fareMatrixService;
        this.validationService = validationService;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public FareMatrixValidationResponse validate(UUID cinemaId, UUID policyId, UUID versionId) {

        return validationService.validate(fareMatrixService.get(cinemaId, policyId, versionId));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CinemaPricingPolicyVersionResponse publish(
            UUID cinemaId,
            UUID policyId,
            UUID versionId,
            PublishPricingPolicyRequest request,
            String actorSubject) {

        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {

            throw new ValidationException(InventoryErrorCode.INVALID_PRICING_POLICY);
        }

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

        if (revision.getStatus() == PricingPolicyVersionStatus.PUBLISHED) {

            return mapper.toResponse(revision);
        }

        revision.requireDraft();

        if (!request.expectedVersion().equals(revision.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        var matrix = fareMatrixService.getForPublication(cinemaId, policyId, versionId);

        var validation = validationService.validate(matrix);

        if (!validation.valid()) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_PUBLISHABLE);
        }

        revision.publish(
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC), actorSubject);

        versionRepository.flush();

        return mapper.toResponse(revision);
    }
}
