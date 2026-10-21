package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaFareMatrixRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.PricingDayGroup;
import com.cinema.inventory.entity.PricingTimeBand;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.PricingDayGroupRepository;
import com.cinema.inventory.repository.PricingTimeBandRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CinemaFareMatrixWriteService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaPricingPolicyMapper policyMapper;
    private final EntityManager entityManager;

    public CinemaFareMatrixWriteService(
            CinemaPricingPolicyRepository policyRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository,
            FareRuleRepository fareRuleRepository,
            CinemaPricingPolicyMapper policyMapper,
            EntityManager entityManager) {

        this.policyRepository = policyRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.policyMapper = policyMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public CinemaPricingPolicyResponse replaceMatrix(
            UUID cinemaId, UUID policyId, ReplaceCinemaFareMatrixRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentUpdate(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireFareMatrixModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        if (!"VND".equals(policy.getCurrency())) {
            throw invalidFare();
        }

        // Validate and construct everything before deleting stored content.
        PreparedMatrix prepared = prepare(policy, request);

        policy.markContentChanged();

        // Remove dependent fare rows before their dimensions.
        fareRuleRepository.deleteAllForPolicy(policy.getId());
        dayGroupRepository.deleteAllForPolicy(policy.getId());
        timeBandRepository.deleteAllForPolicy(policy.getId());

        prepared.dayGroups().forEach(entityManager::persist);
        prepared.timeBands().forEach(entityManager::persist);

        // Ensure dimension rows exist before inserting composite foreign keys.
        entityManager.flush();

        prepared.fareRules().forEach(entityManager::persist);
        entityManager.flush();

        return policyMapper.toResponse(policy);
    }

    private PreparedMatrix prepare(
            CinemaPricingPolicy policy, ReplaceCinemaFareMatrixRequest request) {

        Map<String, PricingDayGroup> dayGroups = new LinkedHashMap<>();

        for (var input : request.dayGroups()) {
            PricingDayGroup dayGroup =
                    new PricingDayGroup(
                            policy,
                            input.code(),
                            input.name(),
                            input.daysOfWeek(),
                            input.includesHolidays());

            if (dayGroups.putIfAbsent(dayGroup.getCode(), dayGroup) != null) {
                throw invalidDimension();
            }
        }

        Map<String, PricingTimeBand> timeBands = new LinkedHashMap<>();

        for (var input : request.timeBands()) {
            PricingTimeBand timeBand =
                    new PricingTimeBand(
                            policy,
                            input.code(),
                            input.name(),
                            input.fromMinute(),
                            input.toMinute());

            if (timeBands.putIfAbsent(timeBand.getCode(), timeBand) != null) {
                throw invalidDimension();
            }
        }

        Set<String> fareCodes = new HashSet<>();
        List<FareRule> fareRules = new ArrayList<>();

        for (var input : request.fareRules()) {
            PricingDayGroup dayGroup = dayGroups.get(referenceCode(input.dayGroupCode()));

            PricingTimeBand timeBand = timeBands.get(referenceCode(input.timeBandCode()));

            if (dayGroup == null || timeBand == null) {
                throw invalidFare();
            }

            FareRule rule =
                    new FareRule(
                            policy,
                            input.code(),
                            input.experienceType(),
                            input.contentFormat(),
                            dayGroup,
                            timeBand,
                            input.ticketCategoryCode(),
                            input.seatTypeCode(),
                            input.firstShowingCondition(),
                            input.requiredEligibilityCode(),
                            parseAmount(input.amountVnd()),
                            input.fareUnit(),
                            input.priority(),
                            input.effectiveFrom(),
                            input.effectiveTo());

            if (!fareCodes.add(rule.getCode())) {
                throw invalidFare();
            }

            fareRules.add(rule);
        }

        return new PreparedMatrix(
                List.copyOf(dayGroups.values()),
                List.copyOf(timeBands.values()),
                List.copyOf(fareRules));
    }

    private static String referenceCode(String value) {
        if (value == null) {
            throw invalidFare();
        }

        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Long parseAmount(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,18}")) {
            throw invalidFare();
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw invalidFare();
        }
    }

    private static ValidationException invalidDimension() {
        return new ValidationException(InventoryErrorCode.PRICING_DIMENSION_DATA_INVALID);
    }

    private static ValidationException invalidFare() {
        return new ValidationException(InventoryErrorCode.FARE_RULE_DATA_INVALID);
    }

    private record PreparedMatrix(
            List<PricingDayGroup> dayGroups,
            List<PricingTimeBand> timeBands,
            List<FareRule> fareRules) {}
}
