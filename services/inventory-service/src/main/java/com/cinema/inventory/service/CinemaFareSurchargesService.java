package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaFareSurchargesRequest;
import com.cinema.inventory.dto.response.CinemaFareSurchargesResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareSurchargeRule;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaFareSurchargesMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareSurchargeRuleRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaFareSurchargesService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final FareSurchargeRuleRepository surchargeRepository;
    private final CinemaFareSurchargesMapper mapper;
    private final EntityManager entityManager;

    public CinemaFareSurchargesService(
            CinemaPricingPolicyRepository policyRepository,
            FareSurchargeRuleRepository surchargeRepository,
            CinemaFareSurchargesMapper mapper,
            EntityManager entityManager) {

        this.policyRepository = policyRepository;
        this.surchargeRepository = surchargeRepository;
        this.mapper = mapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CinemaFareSurchargesResponse getSurcharges(
            UUID cinemaId, UUID policyId) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(() -> new NotFoundException(
                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        return read(policy);
    }

    @Transactional
    public CinemaFareSurchargesResponse replaceSurcharges(
            UUID cinemaId,
            UUID policyId,
            ReplaceCinemaFareSurchargesRequest request) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(() -> new NotFoundException(
                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        if (request.mode() == FareSurchargeMode.NONE
                && !request.surcharges().isEmpty()) {
            throw invalid();
        }

        Set<String> codes = new HashSet<>();
        List<FareSurchargeRule> prepared = new ArrayList<>();

        for (var input : request.surcharges()) {
            FareSurchargeRule rule =
                    new FareSurchargeRule(
                            policy,
                            input.code(),
                            input.fareRuleCode(),
                            input.movieAttributeCode(),
                            input.requiredEligibilityCode(),
                            parseAmount(input.amountVnd()),
                            input.unit(),
                            input.priority(),
                            input.effectiveFrom(),
                            input.effectiveTo());

            if (!codes.add(rule.getCode())) {
                throw invalid();
            }

            prepared.add(rule);
        }

        policy.changeFareSurchargeMode(request.mode());
        policy.markContentChanged();

        surchargeRepository.deleteAllForPolicy(policy.getId());
        prepared.forEach(entityManager::persist);
        entityManager.flush();

        return read(policy);
    }

    private CinemaFareSurchargesResponse read(CinemaPricingPolicy policy) {
        return mapper.toResponse(
                policy,
                surchargeRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAsc(policy.getId()));
    }

    private static Long parseAmount(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) {
            throw invalid();
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private static ValidationException invalid() {
        return new ValidationException(
                InventoryErrorCode.FARE_SURCHARGE_DATA_INVALID);
    }
}
