package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.PublishCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.dto.response.PublishCinemaPricingPolicyResponse;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse.ValidationStatus;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.enums.PricingPolicyStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.service.pricing.DraftFareMatrixValidationService;
import com.cinema.inventory.service.pricing.PricingPolicyPeriod;
import com.cinema.inventory.service.pricing.PublishedCinemaPricingPolicyResolver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CinemaPricingPolicyPublicationService {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final DraftFareMatrixValidationService validationService;
    private final PublishedCinemaPricingPolicyResolver publishedResolver;
    private final CinemaPricingPolicyMapper policyMapper;
    private final Clock clock;

    public CinemaPricingPolicyPublicationService(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository,
            DraftFareMatrixValidationService validationService,
            PublishedCinemaPricingPolicyResolver publishedResolver,
            CinemaPricingPolicyMapper policyMapper,
            Clock clock) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
        this.validationService = validationService;
        this.publishedResolver = publishedResolver;
        this.policyMapper = policyMapper;
        this.clock = clock;
    }

    @Transactional
    public PublishCinemaPricingPolicyResponse publish(
            UUID cinemaId,
            UUID policyId,
            UUID publisherId,
            PublishCinemaPricingPolicyRequest request) {

        requireBusinessConfirmation(request, publisherId);

        // Common lock order: cinema first, then policy.
        cinemaRepository
                .findByIdForUpdate(cinemaId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!policy.getVersion().equals(request.expectedVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        PricingPolicyPeriod proposedPeriod = PricingPolicyPeriod.from(policy);

        List<CinemaPricingPolicy> publishedPolicies =
                policyRepository.findByCinemaAndStatusForRead(
                        cinemaId, PricingPolicyStatus.PUBLISHED);

        for (CinemaPricingPolicy published : publishedPolicies) {
            if (proposedPeriod.overlaps(PricingPolicyPeriod.from(published))) {
                throw new ConflictException(InventoryErrorCode.PRICING_POLICY_PERIOD_OVERLAP);
            }
        }

        /*
         * Re-evaluate the current content under the policy write lock.
         * Do not trust a previous validate response supplied by the client.
         *
         * Nested services join this transaction; the existing write lock
         * remains held until publication commits.
         */
        ValidateDraftFareMatrixResponse validation =
                validationService.validateMatrix(
                        cinemaId,
                        policyId,
                        new ReviewCinemaPricingPolicyRequest(request.expectedVersion()));

        boolean valid =
                validation.status() == ValidationStatus.COMPLETE
                        && validation.matrixValid()
                        && validation.unitPricingValid();

        if (!valid) {
            return new PublishCinemaPricingPolicyResponse(
                    false, policyMapper.toResponse(policy), validation);
        }

        policy.publish(
                OffsetDateTime.now(clock), publisherId, request.businessConfirmationReference());

        // Flush before mapping so the response includes the new lock version.
        policyRepository.flush();

        return new PublishCinemaPricingPolicyResponse(
                true, policyMapper.toResponse(policy), validation);
    }

    @Transactional
    public CinemaPricingPolicyResponse getActive(UUID cinemaId, OffsetDateTime startsAt) {

        return policyMapper.toResponse(publishedResolver.resolveForStart(cinemaId, startsAt));
    }

    private void requireBusinessConfirmation(
            PublishCinemaPricingPolicyRequest request, UUID publisherId) {

        if (request == null
                || publisherId == null
                || !Boolean.TRUE.equals(request.businessConfirmed())
                || request.businessConfirmationReference() == null
                || request.businessConfirmationReference().isBlank()
                || request.businessConfirmationReference().trim().length() > 500) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_POLICY_PUBLICATION_DATA_INVALID);
        }
    }
}
