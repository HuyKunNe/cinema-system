package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.PreviewDraftFareMatrixRequest;
import com.cinema.inventory.dto.response.PreviewDraftFareMatrixResponse;
import com.cinema.inventory.dto.response.PreviewDraftFareMatrixResponse.MatrixPreviewContext;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaFareMatrixMapper;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class DraftFareMatrixPreviewService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final DraftTicketCategorySimulationGuard categorySimulationGuard;
    private final CinemaPricingPolicyMapper policyMapper;
    private final CinemaFareMatrixMapper matrixMapper;
    private final FareRuleSelector ruleSelector;
    private final DraftSeatTypeSimulationGuard seatTypeSimulationGuard;
    private final DraftSaleScopeResolver saleScopeResolver;
    private final DraftFixedFareGuard fixedFareGuard;
    private final FareCategoryResolver categoryResolver;

    public DraftFareMatrixPreviewService(
            CinemaPricingPolicyRepository policyRepository,
            FareRuleRepository fareRuleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaPricingPolicyMapper policyMapper,
            CinemaFareMatrixMapper matrixMapper,
            FareRuleSelector ruleSelector,
            DraftTicketCategorySimulationGuard categorySimulationGuard,
            DraftSeatTypeSimulationGuard seatTypeSimulationGuard,
            DraftSaleScopeResolver saleScopeResolver,
            DraftFixedFareGuard fixedFareGuard,
            FareCategoryResolver categoryResolver) {

        this.policyRepository = policyRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.holidayRepository = holidayRepository;
        this.policyMapper = policyMapper;
        this.matrixMapper = matrixMapper;
        this.ruleSelector = ruleSelector;
        this.categorySimulationGuard = categorySimulationGuard;
        this.seatTypeSimulationGuard = seatTypeSimulationGuard;
        this.saleScopeResolver = saleScopeResolver;
        this.fixedFareGuard = fixedFareGuard;
        this.categoryResolver = categoryResolver;
    }

    @Transactional
    public PreviewDraftFareMatrixResponse preview(
            UUID cinemaId, UUID policyId, PreviewDraftFareMatrixRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
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
            throw new ValidationException(InventoryErrorCode.FARE_RULE_DATA_INVALID);
        }

        fixedFareGuard.requireFixedFares(policy);

        OffsetDateTime localStartsAt = toLocalStartsAt(policy, request.startsAt());

        LocalDate localDate = localStartsAt.toLocalDate();
        requireEffectiveDate(policy, localDate);

        boolean holiday =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId()).stream()
                        .anyMatch(entry -> entry.getHolidayDate().equals(localDate));

        FareRuleEvaluationContext context =
                new FareRuleEvaluationContext(
                        policy.getId(),
                        localStartsAt,
                        holiday,
                        request.experienceType(),
                        request.contentFormat(),
                        request.ticketCategoryCode(),
                        request.seatTypeCode(),
                        request.simulatedReleaseDate(),
                        request.simulatedEligibilityCodes());

        FareCategoryResolver.Resolution categoryResolution = categoryResolver.resolve(context);

        context = categoryResolution.context();

        var category = categorySimulationGuard.requireForSimulation(context);

        PricingSeatType seatType = seatTypeSimulationGuard.requireForSimulation(context);

        CategoryDiscountEligibilityGuard.requireAllowed(category, seatType.getPhysicalSeatType());

        var saleScope = saleScopeResolver.resolve(context);

        if (!saleScope.allowsMinute(context.minuteOfDay())) {
            throw new ConflictException(InventoryErrorCode.PRICING_SCENARIO_NOT_FOR_SALE);
        }

        FareRuleSelector.Selection selection =
                ruleSelector.select(
                        fareRuleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                                policy.getId()),
                        context);
        seatTypeSimulationGuard.requireCompatibleFareUnits(seatType, selection.winners());

        MatrixPreviewContext resolved =
                new MatrixPreviewContext(
                        context.localStartsAt(),
                        context.holiday(),
                        context.experienceType(),
                        context.contentFormat(),
                        context.ticketCategoryCode(),
                        context.seatTypeCode(),
                        context.officialReleaseDate(),
                        context.advanceScreening(),
                        context.eligibilityCodes().stream().sorted().toList(),
                        categoryResolution.requestedTicketCategoryCode(),
                        categoryResolution.reason().name());

        return new PreviewDraftFareMatrixResponse(
                true,
                false,
                false,
                false,
                policyMapper.toResponse(policy),
                resolved,
                selection.status(),
                selection.winners().stream().map(matrixMapper::toFareRuleResponse).toList());
    }

    private OffsetDateTime toLocalStartsAt(CinemaPricingPolicy policy, OffsetDateTime startsAt) {

        String timeZone = policy.getTimeZone();

        if (timeZone == null || timeZone.isBlank()) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        try {
            return startsAt.toInstant().atZone(ZoneId.of(timeZone)).toOffsetDateTime();
        } catch (DateTimeException exception) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }
    }

    private void requireEffectiveDate(CinemaPricingPolicy policy, LocalDate localDate) {

        if (localDate.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !localDate.isBefore(policy.getEffectiveTo()))) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }
    }
}
