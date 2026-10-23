package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse;
import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse.MatrixReviewIssue;
import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse.MatrixReviewSeverity;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.FareSurchargeRule;
import com.cinema.inventory.entity.PricingDayGroup;
import com.cinema.inventory.entity.PricingSaleScope;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.entity.PricingTimeBand;
import com.cinema.inventory.enums.AdvanceScreeningCondition;
import com.cinema.inventory.enums.ExperienceType;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.enums.TicketPricingRole;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.FareSurchargeRuleRepository;
import com.cinema.inventory.repository.PricingDayGroupRepository;
import com.cinema.inventory.repository.PricingSaleScopeRepository;
import com.cinema.inventory.repository.PricingSeatTypeRepository;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;
import com.cinema.inventory.repository.PricingTimeBandRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DraftFareMatrixReviewService {

    private static final int MAX_ISSUES = 200;

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final PricingTicketCategoryRepository categoryRepository;
    private final PricingSaleScopeRepository scopeRepository;
    private final PricingSeatTypeRepository seatTypeRepository;
    private final CinemaPricingPolicyMapper policyMapper;
    private final FareSurchargeRuleRepository fareSurchargeRepository;

    public DraftFareMatrixReviewService(
            CinemaPricingPolicyRepository policyRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository,
            FareRuleRepository fareRuleRepository,
            CinemaHolidayDateRepository holidayRepository,
            PricingTicketCategoryRepository categoryRepository,
            PricingSeatTypeRepository seatTypeRepository,
            CinemaPricingPolicyMapper policyMapper,
            PricingSaleScopeRepository scopeRepository,
            FareSurchargeRuleRepository fareSurchargeRepository) {

        this.policyRepository = policyRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.holidayRepository = holidayRepository;
        this.categoryRepository = categoryRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.policyMapper = policyMapper;
        this.scopeRepository = scopeRepository;
        this.fareSurchargeRepository = fareSurchargeRepository;
    }

    @Transactional
    public CinemaFareMatrixReviewResponse review(
            UUID cinemaId, UUID policyId, ReviewCinemaPricingPolicyRequest request) {

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

        UUID scopedPolicyId = policy.getId();

        List<PricingDayGroup> dayGroups =
                dayGroupRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId);

        List<PricingTimeBand> timeBands =
                timeBandRepository.findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(scopedPolicyId);

        List<FareRule> fareRules =
                fareRuleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(scopedPolicyId);

        List<CinemaHolidayDate> holidays =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(scopedPolicyId);

        List<PricingTicketCategory> ticketCategories =
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId);

        List<PricingSeatType> seatTypes =
                seatTypeRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId);

        List<PricingSaleScope> saleScopes =
                scopeRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId);

        List<FareSurchargeRule> fareSurcharges =
                fareSurchargeRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                        scopedPolicyId);

        IssueCollector report = new IssueCollector();

        checkRequiredData(policy, dayGroups, timeBands, fareRules, report);
        checkPeriodsAndUnits(policy, fareRules, holidays, report);
        checkTicketCategories(ticketCategories, fareRules, report);
        checkCategoryPricingRules(ticketCategories, fareRules, saleScopes, report);
        checkCategoryDiscountSeats(ticketCategories, seatTypes, fareRules, saleScopes, report);
        checkFareSurcharges(policy, fareSurcharges, report);
        checkSeatTypes(seatTypes, fareRules, report);
        checkSaleScopes(saleScopes, dayGroups, timeBands, ticketCategories, seatTypes, report);
        checkDimensionUsage(dayGroups, timeBands, fareRules, holidays, report);

        return new CinemaFareMatrixReviewResponse(
                policyMapper.toResponse(policy),
                report.hasErrors,
                report.truncated,
                false,
                List.copyOf(report.issues),
                List.of(
                        "FARE_MATRIX_FULL_COVERAGE",
                        "HIGHEST_PRIORITY_AMBIGUITIES",
                        "SALE_SCOPE_BUSINESS_CONFIRMATION",
                        "CUSTOMER_ELIGIBILITY",
                        "ELIGIBILITY_RULE_IMPLEMENTATIONS",
                        "SEAT_TYPE_MAPPING_BUSINESS_CONFIRMATION",
                        "PAIR_GROUPING_AND_CHARGING",
                        "EXPERIENCE_AND_CONTENT_COMPATIBILITY",
                        "OFFICIAL_RELEASE_DATE_RESOLUTION",
                        "HOLIDAY_CALENDAR_COMPLETENESS",
                        "PUBLISHED_POLICY_PERIOD_CONFLICTS",
                        "REFERENCE_TARIFF_BUSINESS_CONFIRMATION"));
    }

    private void checkSaleScopes(
            List<PricingSaleScope> scopes,
            List<PricingDayGroup> dayGroups,
            List<PricingTimeBand> timeBands,
            List<PricingTicketCategory> categories,
            List<PricingSeatType> seatTypes,
            IssueCollector report) {

        if (scopes.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "SALE_SCOPE_EMPTY",
                    "Declare which combinations and time windows the policy sells.");
        }

        Set<String> dayCodes =
                dayGroups.stream().map(PricingDayGroup::getCode).collect(Collectors.toSet());

        Set<String> timeCodes =
                timeBands.stream().map(PricingTimeBand::getCode).collect(Collectors.toSet());

        Set<String> categoryCodes =
                categories.stream().map(PricingTicketCategory::getCode).collect(Collectors.toSet());

        Set<String> seatCodes =
                seatTypes.stream().map(PricingSeatType::getCode).collect(Collectors.toSet());

        for (PricingSaleScope scope : scopes) {
            checkScopeReference(
                    dayCodes,
                    scope.getDayGroupCode(),
                    "SALE_SCOPE_DAY_GROUP_UNKNOWN",
                    scope.getId(),
                    report);

            checkScopeReference(
                    timeCodes,
                    scope.getTimeBandCode(),
                    "SALE_SCOPE_TIME_BAND_UNKNOWN",
                    scope.getId(),
                    report);

            checkScopeReference(
                    categoryCodes,
                    scope.getTicketCategoryCode(),
                    "SALE_SCOPE_TICKET_CATEGORY_UNKNOWN",
                    scope.getId(),
                    report);

            checkScopeReference(
                    seatCodes,
                    scope.getSeatTypeCode(),
                    "SALE_SCOPE_SEAT_TYPE_UNKNOWN",
                    scope.getId(),
                    report);
        }
    }

    private void checkScopeReference(
            Set<String> knownCodes,
            String referencedCode,
            String issueCode,
            UUID scopeId,
            IssueCollector report) {

        if (!knownCodes.contains(referencedCode)) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    issueCode,
                    "The sale scope references a code absent from this policy catalog.",
                    scopeId);
        }
    }

    private void checkRequiredData(
            CinemaPricingPolicy policy,
            List<PricingDayGroup> dayGroups,
            List<PricingTimeBand> timeBands,
            List<FareRule> fareRules,
            IssueCollector report) {

        if (!"VND".equals(policy.getCurrency())) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "UNSUPPORTED_MATRIX_CURRENCY",
                    "The current fare matrix implementation supports VND only.");
        }

        if (dayGroups.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "DAY_GROUPS_EMPTY",
                    "The matrix has no day groups.");
        }

        if (timeBands.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "TIME_BANDS_EMPTY",
                    "The matrix has no time bands.");
        }

        if (fareRules.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "FARE_RULES_EMPTY",
                    "The matrix has no fare rules.");
        }
    }

    private void checkPeriodsAndUnits(
            CinemaPricingPolicy policy,
            List<FareRule> fareRules,
            List<CinemaHolidayDate> holidays,
            IssueCollector report) {

        for (FareRule rule : fareRules) {
            if (rule.getFareUnit() == FareUnit.UNCONFIRMED) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_UNIT_UNCONFIRMED",
                        "Confirm whether the fare amount applies per ticket or per pair.",
                        rule.getId());
            }

            if (periodOutsidePolicy(policy, rule.getEffectiveFrom(), rule.getEffectiveTo())) {

                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_PERIOD_OUTSIDE_POLICY",
                        "The fare effective period is outside the policy period.",
                        rule.getId());
            }
        }

        for (CinemaHolidayDate holiday : holidays) {
            if (dateOutsidePolicy(policy, holiday.getHolidayDate())) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "HOLIDAY_OUTSIDE_POLICY_PERIOD",
                        "The holiday date is outside the policy effective period.",
                        holiday.getId());
            }
        }
    }

    private void checkTicketCategories(
            List<PricingTicketCategory> ticketCategories,
            List<FareRule> fareRules,
            IssueCollector report) {

        if (ticketCategories.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "TICKET_CATEGORY_CATALOG_EMPTY",
                    "The policy has no ticket category definitions.");
        }

        Map<String, PricingTicketCategory> categoriesByCode = new HashMap<>();

        for (PricingTicketCategory category : ticketCategories) {
            categoriesByCode.put(category.getCode(), category);

            if (category.getPricingRole() == TicketPricingRole.CATEGORY_DISCOUNT
                    && category.getEligibilityMode() != TicketEligibilityMode.RULE_REQUIRED) {

                report.add(
                        MatrixReviewSeverity.ERROR,
                        "CATEGORY_DISCOUNT_REQUIRES_ELIGIBILITY",
                        "Category discounts require verified attendee eligibility.",
                        category.getId());
            }

            if (category.getEligibilityMode() == TicketEligibilityMode.UNCONFIRMED) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "TICKET_CATEGORY_ELIGIBILITY_UNCONFIRMED",
                        "Confirm the eligibility requirements of this ticket category.",
                        category.getId());
            }
        }

        Set<String> usedCategoryCodes = new HashSet<>();

        for (FareRule rule : fareRules) {
            String categoryCode = rule.getTicketCategoryCode();
            usedCategoryCodes.add(categoryCode);

            if (!categoriesByCode.containsKey(categoryCode)) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_TICKET_CATEGORY_UNKNOWN",
                        "The fare references a ticket category absent from this policy catalog.",
                        rule.getId());
            }
        }

        // Emit warnings after checking all category errors.
        for (PricingTicketCategory category : ticketCategories) {
            if (!usedCategoryCodes.contains(category.getCode())) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "TICKET_CATEGORY_WITHOUT_FARES",
                        "The ticket category is not referenced by any fare rule.",
                        category.getId());
            }

            if (category.getEligibilityMode() == TicketEligibilityMode.RULE_REQUIRED) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "CATEGORY_ELIGIBILITY_RULE_UNVERIFIED",
                        "This category requires an eligibility rule. "
                                + "Its runtime implementation and customer evidence "
                                + "are not verified by this review.",
                        category.getId());
            }
        }
    }

    private void checkSeatTypes(
            List<PricingSeatType> seatTypes, List<FareRule> fareRules, IssueCollector report) {

        if (seatTypes.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "SEAT_TYPE_CATALOG_EMPTY",
                    "The policy has no pricing seat type definitions.");
        }

        Map<String, PricingSeatType> seatTypesByCode = new HashMap<>();

        for (PricingSeatType seatType : seatTypes) {
            seatTypesByCode.put(seatType.getCode(), seatType);

            if (seatType.getMappingStatus() == PricingSeatMappingStatus.UNCONFIRMED) {

                report.add(
                        MatrixReviewSeverity.ERROR,
                        "SEAT_TYPE_MAPPING_UNCONFIRMED",
                        "Confirm the physical seat type mapping of this pricing definition.",
                        seatType.getId());
            }
        }

        Set<String> usedSeatTypeCodes = new HashSet<>();

        for (FareRule rule : fareRules) {
            String seatTypeCode = rule.getSeatTypeCode();
            usedSeatTypeCodes.add(seatTypeCode);

            PricingSeatType seatType = seatTypesByCode.get(seatTypeCode);

            if (seatType == null) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_SEAT_TYPE_UNKNOWN",
                        "The fare references a seat type absent from this policy catalog.",
                        rule.getId());
            } else if (!seatType.acceptsFareUnit(rule.getFareUnit())) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_PAIR_UNIT_REQUIRED",
                        "Fares mapped to physical COUPLE seats must use PER_PAIR.",
                        rule.getId(),
                        seatType.getId());
            }
        }

        // Emit warnings after checking seat catalog and reference errors.
        for (PricingSeatType seatType : seatTypes) {
            if (!usedSeatTypeCodes.contains(seatType.getCode())) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "SEAT_TYPE_WITHOUT_FARES",
                        "The pricing seat type is not referenced by any fare rule.",
                        seatType.getId());
            }
        }

        for (FareRule rule : fareRules) {
            if (rule.getFareUnit() == FareUnit.PER_PAIR) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "PAIR_FARE_GROUPING_UNVERIFIED",
                        "This fare applies per pair. "
                                + "Physical mapping does not verify passenger grouping, "
                                + "ticket allocation or charging behavior.",
                        rule.getId());
            }
        }
    }

    private void checkFareSurcharges(
            CinemaPricingPolicy policy, List<FareSurchargeRule> surcharges, IssueCollector report) {

        if (policy.getFareSurchargeMode() != FareSurchargeMode.NONE || !surcharges.isEmpty()) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "FIXED_FARES_REQUIRED",
                    "V2 fares must be all-inclusive. "
                            + "Confirm fare amounts and clear legacy surcharge configuration.");
        }
    }

    private void checkCategoryPricingRules(
            List<PricingTicketCategory> categories,
            List<FareRule> fares,
            List<PricingSaleScope> scopes,
            IssueCollector report) {

        Map<String, PricingTicketCategory> byCode = new HashMap<>();
        long regularCount = 0;

        Map<String, TicketPricingRole> eligibilityRoles = new HashMap<>();

        for (PricingTicketCategory category : categories) {
            byCode.put(category.getCode(), category);

            TicketPricingRole role = category.getPricingRole();

            if (role == null || role == TicketPricingRole.UNCONFIRMED) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "TICKET_PRICING_ROLE_UNCONFIRMED",
                        "Confirm the pricing role of this category.",
                        category.getId());
                continue;
            }

            if (role == TicketPricingRole.REGULAR) {
                regularCount++;

                if (category.getEligibilityMode() != TicketEligibilityMode.UNRESTRICTED) {
                    report.add(
                            MatrixReviewSeverity.ERROR,
                            "REGULAR_CATEGORY_MUST_BE_UNRESTRICTED",
                            "The regular price category must not require eligibility.",
                            category.getId());
                }
            }

            String eligibilityCode = category.getEligibilityRuleCode();

            if (eligibilityCode != null) {
                TicketPricingRole previous = eligibilityRoles.putIfAbsent(eligibilityCode, role);

                if (previous != null && previous != role) {
                    report.add(
                            MatrixReviewSeverity.ERROR,
                            "ELIGIBILITY_CODE_PRICING_ROLE_CONFLICT",
                            "An eligibility code cannot identify different pricing roles.",
                            category.getId());
                }
            }
        }

        if (regularCount != 1) {
            report.add(
                    MatrixReviewSeverity.ERROR,
                    "EXACTLY_ONE_REGULAR_CATEGORY_REQUIRED",
                    "The policy must define exactly one regular price category.");
        }

        for (FareRule fare : fares) {
            PricingTicketCategory category = byCode.get(fare.getTicketCategoryCode());

            if (fare.getAdvanceScreeningCondition() == AdvanceScreeningCondition.ANY) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "FARE_SCREENING_CONDITION_EXPLICIT_REQUIRED",
                        "Declare REGULAR_SCREENING or ADVANCE_SCREENING explicitly.",
                        fare.getId());
            }

            if (category == null
                    || category.getPricingRole() == null
                    || category.getPricingRole() == TicketPricingRole.UNCONFIRMED) {
                continue;
            }

            TicketPricingRole role = category.getPricingRole();

            if (fare.getExperienceType() == ExperienceType.PREMIUM_CINEMA
                    && role == TicketPricingRole.CATEGORY_DISCOUNT) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "PREMIUM_CATEGORY_DISCOUNT_NOT_ALLOWED",
                        "Use the regular category fare for Premium Cinema.",
                        fare.getId());
            }

            if (role.excludedFromAdvanceScreening()
                    && fare.getAdvanceScreeningCondition()
                            != AdvanceScreeningCondition.REGULAR_SCREENING) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "PROGRAM_FARE_REGULAR_SCREENING_ONLY",
                        "C'Monday, C'Member and C'Ten fares cannot apply to advance screenings.",
                        fare.getId());
            }

            if (role == TicketPricingRole.REGULAR && fare.getRequiredEligibilityCode() != null) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "REGULAR_FARE_MUST_NOT_REQUIRE_ELIGIBILITY",
                        "Represent promotional prices with their own pricing category.",
                        fare.getId());
            }
        }

        for (PricingSaleScope scope : scopes) {
            PricingTicketCategory category = byCode.get(scope.getTicketCategoryCode());

            if (category == null
                    || category.getPricingRole() == null
                    || category.getPricingRole() == TicketPricingRole.UNCONFIRMED) {
                continue;
            }

            TicketPricingRole role = category.getPricingRole();

            if (scope.getExperienceType() == ExperienceType.PREMIUM_CINEMA
                    && role == TicketPricingRole.CATEGORY_DISCOUNT) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "PREMIUM_DISCOUNT_SCOPE_NOT_ALLOWED",
                        "Declare the regular category sale scope for Premium Cinema.",
                        scope.getId());
            }

            if (role.excludedFromAdvanceScreening()
                    && scope.getAdvanceScreeningCondition()
                            != AdvanceScreeningCondition.REGULAR_SCREENING) {
                report.add(
                        MatrixReviewSeverity.ERROR,
                        "PROGRAM_SCOPE_REGULAR_SCREENING_ONLY",
                        "These promotional scopes must exclude advance screenings.",
                        scope.getId());
            }
        }
    }

    private void checkDimensionUsage(
            List<PricingDayGroup> dayGroups,
            List<PricingTimeBand> timeBands,
            List<FareRule> fareRules,
            List<CinemaHolidayDate> holidays,
            IssueCollector report) {

        Set<UUID> usedDayGroups = new HashSet<>();
        Set<UUID> usedTimeBands = new HashSet<>();

        for (FareRule rule : fareRules) {
            usedDayGroups.add(rule.getDayGroup().getId());
            usedTimeBands.add(rule.getTimeBand().getId());
        }

        for (PricingDayGroup dayGroup : dayGroups) {
            if (!usedDayGroups.contains(dayGroup.getId())) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "DAY_GROUP_UNUSED",
                        "The day group is not referenced by any fare rule.",
                        dayGroup.getId());
            } else if (dayGroup.isIncludesHolidays() && holidays.isEmpty()) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "HOLIDAY_GROUP_WITHOUT_DATES",
                        "A used day group accepts holidays, but the policy calendar is empty.",
                        dayGroup.getId());
            }
        }

        for (PricingTimeBand timeBand : timeBands) {
            if (!usedTimeBands.contains(timeBand.getId())) {
                report.add(
                        MatrixReviewSeverity.WARNING,
                        "TIME_BAND_UNUSED",
                        "The time band is not referenced by any fare rule.",
                        timeBand.getId());
            }
        }
    }

    private void checkCategoryDiscountSeats(
            List<PricingTicketCategory> ticketCategories,
            List<PricingSeatType> seatTypes,
            List<FareRule> fareRules,
            List<PricingSaleScope> saleScopes,
            IssueCollector report) {

        Set<String> discountCodes =
                ticketCategories.stream()
                        .filter(
                                category ->
                                        category.getPricingRole()
                                                == TicketPricingRole.CATEGORY_DISCOUNT)
                        .map(PricingTicketCategory::getCode)
                        .collect(Collectors.toSet());

        Set<String> coupleCodes =
                seatTypes.stream()
                        .filter(
                                type ->
                                        type.getMappingStatus()
                                                == PricingSeatMappingStatus.CONFIRMED)
                        .filter(type -> type.getPhysicalSeatType() == SeatType.COUPLE)
                        .map(PricingSeatType::getCode)
                        .collect(Collectors.toSet());

        for (FareRule fare : fareRules) {
            if (discountCodes.contains(fare.getTicketCategoryCode())
                    && coupleCodes.contains(fare.getSeatTypeCode())) {

                report.add(
                        MatrixReviewSeverity.ERROR,
                        "CATEGORY_DISCOUNT_COUPLE_NOT_ALLOWED",
                        "Category discounts cannot be configured for COUPLE seats.",
                        fare.getId());
            }
        }

        for (PricingSaleScope scope : saleScopes) {
            if (discountCodes.contains(scope.getTicketCategoryCode())
                    && coupleCodes.contains(scope.getSeatTypeCode())) {

                report.add(
                        MatrixReviewSeverity.ERROR,
                        "CATEGORY_DISCOUNT_COUPLE_NOT_ALLOWED",
                        "Category discount sale scopes cannot include COUPLE seats.",
                        scope.getId());
            }
        }
    }

    private boolean dateOutsidePolicy(CinemaPricingPolicy policy, LocalDate date) {

        return date.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null && !date.isBefore(policy.getEffectiveTo()));
    }

    private boolean periodOutsidePolicy(CinemaPricingPolicy policy, LocalDate from, LocalDate to) {

        if (from.isBefore(policy.getEffectiveFrom()) || (to != null && !to.isAfter(from))) {
            return true;
        }

        LocalDate policyEnd = policy.getEffectiveTo();

        return policyEnd != null
                && (!from.isBefore(policyEnd) || to == null || to.isAfter(policyEnd));
    }

    private static final class IssueCollector {

        private final List<MatrixReviewIssue> issues = new ArrayList<>();
        private boolean hasErrors;
        private boolean truncated;

        private void add(
                MatrixReviewSeverity severity, String code, String message, UUID... relatedIds) {

            // Keep the error flag accurate even after the response limit.
            if (severity == MatrixReviewSeverity.ERROR) {
                hasErrors = true;
            }

            if (issues.size() >= MAX_ISSUES) {
                truncated = true;
                return;
            }

            issues.add(new MatrixReviewIssue(severity, code, message, List.of(relatedIds)));
        }
    }
}
