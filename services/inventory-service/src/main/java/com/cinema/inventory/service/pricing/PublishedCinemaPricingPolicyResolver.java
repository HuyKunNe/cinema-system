package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.enums.PricingPolicyStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.CinemaRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PublishedCinemaPricingPolicyResolver {

    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyRepository policyRepository;

    public PublishedCinemaPricingPolicyResolver(
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyRepository policyRepository) {

        this.cinemaRepository = cinemaRepository;
        this.policyRepository = policyRepository;
    }

    /**
     * Caller must supply a transaction.
     * Locks cinema before reading published policy versions.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CinemaPricingPolicy resolveForStart(
            UUID cinemaId,
            OffsetDateTime startsAt) {

        if (cinemaId == null || startsAt == null) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        cinemaRepository.findByIdForRead(cinemaId)
                .orElseThrow(() ->
                        new NotFoundException(
                                InventoryErrorCode.CINEMA_NOT_FOUND));

        List<CinemaPricingPolicy> publishedPolicies =
                policyRepository.findByCinemaAndStatusForRead(
                        cinemaId,
                        PricingPolicyStatus.PUBLISHED);

        Instant requestedStart = startsAt.toInstant();

        List<CinemaPricingPolicy> matches =
                publishedPolicies.stream()
                        .filter(policy ->
                                PricingPolicyPeriod.from(policy)
                                        .contains(requestedStart))
                        .toList();

        if (matches.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_PUBLISHED_POLICY_REQUIRED);
        }

        if (matches.size() > 1) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_PUBLISHED_POLICY_AMBIGUOUS);
        }

        CinemaPricingPolicy policy = matches.getFirst();
        policy.requirePublishedConfiguration();

        return policy;
    }
}
