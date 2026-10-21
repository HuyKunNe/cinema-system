package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse.MatrixFinding;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse.ValidationStatus;
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
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.enums.FareSurchargeMode;
import com.cinema.inventory.enums.FareSurchargeUnit;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.exception.InventoryErrorCode;
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

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DraftFareMatrixValidationService {

    // Resource budgets, not pricing/business limits.
    private static final long MAX_DATE_VISITS = 100_000;
    private static final long MAX_CELLS = 100_000;
    private static final long MAX_RULE_EVALUATIONS = 2_000_000;
    private static final int MAX_FINDINGS = 200;

    private final DraftFareMatrixReviewService reviewService;
    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingSaleScopeRepository scopeRepository;
    private final FareRuleRepository fareRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;
    private final PricingTicketCategoryRepository categoryRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final FareSurchargeRuleRepository surchargeRepository;
    private final PricingSeatTypeRepository seatTypeRepository;
    private final FareCategoryResolver categoryResolver;
    private final FareRuleSelector selector;

    public DraftFareMatrixValidationService(
            DraftFareMatrixReviewService reviewService,
            CinemaPricingPolicyRepository policyRepository,
            PricingSaleScopeRepository scopeRepository,
            FareRuleRepository fareRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository,
            PricingTicketCategoryRepository categoryRepository,
            CinemaHolidayDateRepository holidayRepository,
            FareRuleSelector selector,
            FareSurchargeRuleRepository surchargeRepository,
            PricingSeatTypeRepository seatTypeRepository,
            FareCategoryResolver categoryResolver) {

        this.reviewService = reviewService;
        this.policyRepository = policyRepository;
        this.scopeRepository = scopeRepository;
        this.fareRepository = fareRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
        this.categoryRepository = categoryRepository;
        this.holidayRepository = holidayRepository;
        this.selector = selector;
        this.surchargeRepository = surchargeRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.categoryResolver = categoryResolver;
    }

    @Transactional
    public ValidateDraftFareMatrixResponse validateMatrix(
            UUID cinemaId, UUID policyId, ReviewCinemaPricingPolicyRequest request) {

        // Joins this transaction and holds the parent policy read lock.
        // Also checks cinema scope, V2, draft and expectedVersion.
        CinemaFareMatrixReviewResponse review = reviewService.review(cinemaId, policyId, request);

        Report report = new Report();

        if (review.hasErrors()) {
            return response(review, ValidationStatus.CONFIGURATION_ERRORS, report);
        }

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        List<PricingSaleScope> scopes = scopeRepository.findAllByPolicy_IdOrderByCodeAsc(policyId);

        List<FareRule> allRules =
                fareRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policyId);

        List<FareSurchargeRule> allSurcharges =
                surchargeRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policyId);

        Map<String, PricingSeatType> seatTypes =
                seatTypeRepository.findAllByPolicy_IdOrderByCodeAsc(policyId).stream()
                        .collect(Collectors.toMap(PricingSeatType::getCode, Function.identity()));

        Map<String, PricingDayGroup> dayGroups =
                dayGroupRepository.findAllByPolicy_IdOrderByCodeAsc(policyId).stream()
                        .collect(Collectors.toMap(PricingDayGroup::getCode, Function.identity()));

        Map<String, PricingTimeBand> timeBands =
                timeBandRepository.findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(policyId).stream()
                        .collect(Collectors.toMap(PricingTimeBand::getCode, Function.identity()));

        Map<String, PricingTicketCategory> categories =
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(policyId).stream()
                        .collect(
                                Collectors.toMap(
                                        PricingTicketCategory::getCode, Function.identity()));

        List<PricingTicketCategory> categorySnapshot = List.copyOf(categories.values());

        Set<LocalDate> holidays =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policyId).stream()
                        .map(CinemaHolidayDate::getHolidayDate)
                        .collect(Collectors.toSet());

        List<LocalDate> dates = representativeDates(policy, allRules, allSurcharges, holidays);

        ValidationStatus status;

        try {
            for (PricingSaleScope scope : scopes) {
                report.spendRules(allRules.size());

                List<FareRule> bucket =
                        allRules.stream().filter(rule -> sameDimensions(scope, rule)).toList();

                PricingDayGroup scopeDay = dayGroups.get(scope.getDayGroupCode());

                PricingTimeBand scopeTime = timeBands.get(scope.getTimeBandCode());

                PricingTicketCategory category = categories.get(scope.getTicketCategoryCode());

                // References and eligibility mode were checked by review.
                Set<String> baseEligibility =
                        category.getEligibilityMode() == TicketEligibilityMode.RULE_REQUIRED
                                ? Set.of(category.getEligibilityRuleCode())
                                : Set.of();

                List<Integer> minutes = boundaries(scopeTime, bucket);

                for (LocalDate date : dates) {
                    report.visitDate();

                    boolean holiday = holidays.contains(date);

                    if (!scopeDay.matches(date, holiday)) {
                        continue;
                    }

                    for (boolean advance : screeningStates(scope.getAdvanceScreeningCondition())) {

                        // No representable later release date exists at LocalDate.MAX.
                        if (advance && date.equals(LocalDate.MAX)) {
                            continue;
                        }

                        LocalDate releaseDate = advance ? date.plusDays(1) : date;

                        FareRuleEvaluationContext context =
                                new FareRuleEvaluationContext(
                                        policyId,
                                        date.atStartOfDay().atOffset(ZoneOffset.UTC),
                                        holiday,
                                        scope.getExperienceType(),
                                        scope.getContentFormat(),
                                        scope.getTicketCategoryCode(),
                                        scope.getSeatTypeCode(),
                                        releaseDate,
                                        baseEligibility);

                        for (int index = 0; index < minutes.size() - 1; index++) {
                            int from = minutes.get(index);
                            int to = minutes.get(index + 1);

                            report.visitCell();
                            report.spendRules(bucket.size());

                            List<FareRule> candidates =
                                    bucket.stream()
                                            .filter(
                                                    rule ->
                                                            selector.matchesDimensions(
                                                                    rule, context, from))
                                            .toList();

                            Cell cell = new Cell(scope, from, to);

                            // The minimal eligible state establishes universal coverage
                            // because fare predicates are positive requirements.
                            inspect(candidates, context, cell, true, report, categorySnapshot);

                            inspectSurcharges(
                                    policy,
                                    candidates,
                                    allSurcharges,
                                    seatTypes.get(scope.getSeatTypeCode()),
                                    context,
                                    cell,
                                    report);

                            List<String> optionalCodes =
                                    candidates.stream()
                                            .map(FareRule::getRequiredEligibilityCode)
                                            .filter(Objects::nonNull)
                                            .filter(code -> !baseEligibility.contains(code))
                                            .distinct()
                                            .sorted()
                                            .toList();

                            // One or two extra codes cover minimal witnesses for
                            // a tied highest-priority pair in the current model.
                            for (int first = 0; first < optionalCodes.size(); first++) {
                                Set<String> single = new HashSet<>(baseEligibility);
                                single.add(optionalCodes.get(first));

                                inspect(
                                        candidates,
                                        withEligibility(context, single),
                                        cell,
                                        false,
                                        report,
                                        categorySnapshot);

                                for (int second = first + 1;
                                        second < optionalCodes.size();
                                        second++) {

                                    Set<String> pair = new HashSet<>(single);
                                    pair.add(optionalCodes.get(second));

                                    inspect(
                                            candidates,
                                            withEligibility(context, pair),
                                            cell,
                                            false,
                                            report,
                                            categorySnapshot);
                                }
                            }
                        }
                    }
                }
            }

            status = ValidationStatus.COMPLETE;
        } catch (AnalysisLimitReached exception) {
            status = ValidationStatus.ANALYSIS_LIMIT_REACHED;
        }

        return response(review, status, report);
    }

    private void inspect(
            List<FareRule> candidates,
            FareRuleEvaluationContext context,
            Cell cell,
            boolean checkCoverage,
            Report report,
            List<PricingTicketCategory> categorySnapshot) {

        report.spendRules(categorySnapshot.size());

        FareRuleEvaluationContext effectiveContext =
                categoryResolver.resolve(context, categorySnapshot).context();

        report.spendRules(candidates.size());

        FareRuleSelector.Selection selection =
                selector.selectAtLocalMinute(candidates, effectiveContext, cell.fromMinute());

        if (checkCoverage && selection.status() == FareRuleSelectionStatus.GAP) {
            report.add("GAP", cell, effectiveContext, selection.winners());
        } else if (selection.status() == FareRuleSelectionStatus.AMBIGUOUS) {
            report.add("AMBIGUOUS", cell, effectiveContext, selection.winners());
        }
    }

    private void inspectSurcharges(
            CinemaPricingPolicy policy,
            List<FareRule> candidates,
            List<FareSurchargeRule> allSurcharges,
            PricingSeatType seatType,
            FareRuleEvaluationContext baseContext,
            Cell cell,
            Report report) {

        for (FareRule fare : candidates) {
            Set<String> fareCodes = new HashSet<>(baseContext.eligibilityCodes());

            if (fare.getRequiredEligibilityCode() != null) {
                fareCodes.add(fare.getRequiredEligibilityCode());
            }

            FareRuleEvaluationContext fareContext = withEligibility(baseContext, fareCodes);

            // A positive predicate can add matching rules but cannot remove one.
            // If this fare cannot win uniquely in its minimal state,
            // it cannot become a unique winner in a larger state.
            if (!winsFare(candidates, fareContext, cell, fare, report)) {
                continue;
            }

            report.spendRules(allSurcharges.size());

            List<FareSurchargeRule> active =
                    allSurcharges.stream()
                            .filter(
                                    rule ->
                                            rule.getFareRuleCode().equals(fare.getCode())
                                                    && !fareContext
                                                            .localDate()
                                                            .isBefore(rule.getEffectiveFrom())
                                                    && (rule.getEffectiveTo() == null
                                                            || fareContext
                                                                    .localDate()
                                                                    .isBefore(
                                                                            rule.getEffectiveTo())))
                            .toList();

            report.spendRules(active.size());

            if (!amountBoundFits(policy.getFareSurchargeMode(), fare, active)) {
                report.addSurcharge(
                        "SURCHARGE_AMOUNT_BOUND_UNPROVEN",
                        cell,
                        fareContext,
                        fare,
                        Set.of(),
                        active.stream().map(FareSurchargeRule::getId).toList());
                continue;
            }

            // Includes unconditional surcharges and the no-extra-charge state.
            inspectSurchargeCase(
                    policy,
                    candidates,
                    allSurcharges,
                    seatType,
                    fareContext,
                    Set.of(),
                    fare,
                    cell,
                    report);

            for (int first = 0; first < active.size(); first++) {
                FareSurchargeRule left = active.get(first);

                Set<String> eligibility = new HashSet<>(fareCodes);
                Set<String> movieCodes = new HashSet<>();

                addConditionCodes(left, eligibility, movieCodes);

                inspectSurchargeCase(
                        policy,
                        candidates,
                        allSurcharges,
                        seatType,
                        withEligibility(baseContext, eligibility),
                        movieCodes,
                        fare,
                        cell,
                        report);

                if (policy.getFareSurchargeMode() != FareSurchargeMode.HIGHEST_PRIORITY) {
                    continue;
                }

                for (int second = first + 1; second < active.size(); second++) {
                    report.spendRules(1);

                    FareSurchargeRule right = active.get(second);

                    if (left.getPriority() != right.getPriority()) {
                        continue;
                    }

                    Set<String> pairEligibility = new HashSet<>(eligibility);
                    Set<String> pairMovies = new HashSet<>(movieCodes);

                    addConditionCodes(right, pairEligibility, pairMovies);

                    inspectSurchargeCase(
                            policy,
                            candidates,
                            allSurcharges,
                            seatType,
                            withEligibility(baseContext, pairEligibility),
                            pairMovies,
                            fare,
                            cell,
                            report);
                }
            }
        }
    }

    private boolean winsFare(
            List<FareRule> candidates,
            FareRuleEvaluationContext context,
            Cell cell,
            FareRule expected,
            Report report) {

        report.spendRules(candidates.size());

        FareRuleSelector.Selection selection =
                selector.selectAtLocalMinute(candidates, context, cell.fromMinute());

        return selection.status() == FareRuleSelectionStatus.MATCHED
                && selection.winners().getFirst().getId().equals(expected.getId());
    }

    private void inspectSurchargeCase(
            CinemaPricingPolicy policy,
            List<FareRule> candidates,
            List<FareSurchargeRule> snapshot,
            PricingSeatType seatType,
            FareRuleEvaluationContext context,
            Set<String> movieCodes,
            FareRule expectedFare,
            Cell cell,
            Report report) {

        // Charge the budget before performing work.
        report.spendRules(candidates.size() + 2 * snapshot.size());

        FareRuleSelector.Selection selection =
                selector.selectAtLocalMinute(candidates, context, cell.fromMinute());

        if (selection.status() != FareRuleSelectionStatus.MATCHED
                || !selection.winners().getFirst().getId().equals(expectedFare.getId())) {
            return;
        }

        var evaluation =
                surchargeEvaluator
                        .prepareFromSnapshot(policy, context, seatType, movieCodes, snapshot)
                        .evaluate(selection);

        switch (evaluation.status()) {
            case NONE, MATCHED -> {
                // A complete amount for one fare unit is available.
            }
            default ->
                    report.addSurcharge(
                            "SURCHARGE_" + evaluation.status().name(),
                            cell,
                            context,
                            expectedFare,
                            movieCodes,
                            evaluation.selectedRules().stream().map(rule -> rule.id()).toList());
        }
    }

    private void addConditionCodes(
            FareSurchargeRule rule, Set<String> eligibility, Set<String> movieCodes) {

        if (rule.getRequiredEligibilityCode() != null) {
            eligibility.add(rule.getRequiredEligibilityCode());
        }

        if (rule.getMovieAttributeCode() != null) {
            movieCodes.add(rule.getMovieAttributeCode());
        }
    }

    private boolean amountBoundFits(
            FareSurchargeMode mode, FareRule fare, List<FareSurchargeRule> active) {

        BigInteger additional = BigInteger.ZERO;

        for (FareSurchargeRule rule : active) {
            boolean sameUnit =
                    (fare.getFareUnit() == FareUnit.PER_TICKET
                                    && rule.getUnit() == FareSurchargeUnit.PER_TICKET)
                            || (fare.getFareUnit() == FareUnit.PER_PAIR
                                    && rule.getUnit() == FareSurchargeUnit.PER_PAIR);

            if (!sameUnit) {
                // Unit conversions are checked by scenario evaluation.
                continue;
            }

            BigInteger amount = BigInteger.valueOf(rule.getAmountVnd());

            if (mode == FareSurchargeMode.ADD_ALL) {
                additional = additional.add(amount);
            } else if (mode == FareSurchargeMode.HIGHEST_PRIORITY) {
                additional = additional.max(amount);
            }
        }

        return BigInteger.valueOf(fare.getAmountVnd())
                        .add(additional)
                        .compareTo(BigInteger.valueOf(Long.MAX_VALUE))
                <= 0;
    }

    private boolean sameDimensions(PricingSaleScope scope, FareRule rule) {
        return scope.getExperienceType() == rule.getExperienceType()
                && scope.getContentFormat() == rule.getContentFormat()
                && scope.getTicketCategoryCode().equals(rule.getTicketCategoryCode())
                && scope.getSeatTypeCode().equals(rule.getSeatTypeCode());
    }

    private static boolean[] screeningStates(AdvanceScreeningCondition condition) {

        return switch (condition) {
            case ANY -> new boolean[] {false, true};
            case ADVANCE_SCREENING -> new boolean[] {true};
            case REGULAR_SCREENING -> new boolean[] {false};
        };
    }

    private FareRuleEvaluationContext withEligibility(
            FareRuleEvaluationContext source, Set<String> codes) {

        return new FareRuleEvaluationContext(
                source.policyId(),
                source.localStartsAt(),
                source.holiday(),
                source.experienceType(),
                source.contentFormat(),
                source.ticketCategoryCode(),
                source.seatTypeCode(),
                source.officialReleaseDate(),
                codes);
    }

    private List<Integer> boundaries(PricingTimeBand scopeTime, List<FareRule> rules) {

        TreeSet<Integer> points = new TreeSet<>();
        int from = scopeTime.getFromMinute();
        int to = scopeTime.getToMinute();

        points.add(from);
        points.add(to);

        for (FareRule rule : rules) {
            int ruleFrom = rule.getTimeBand().getFromMinute();
            int ruleTo = rule.getTimeBand().getToMinute();

            if (ruleFrom > from && ruleFrom < to) {
                points.add(ruleFrom);
            }

            if (ruleTo > from && ruleTo < to) {
                points.add(ruleTo);
            }
        }

        return new ArrayList<>(points);
    }

    private List<LocalDate> representativeDates(
            CinemaPricingPolicy policy,
            List<FareRule> rules,
            List<FareSurchargeRule> surcharges,
            Set<LocalDate> holidays) {

        TreeSet<LocalDate> cuts = new TreeSet<>();
        cuts.add(policy.getEffectiveFrom());

        for (FareRule rule : rules) {
            addDateCut(cuts, policy, rule.getEffectiveFrom());
            addDateCut(cuts, policy, rule.getEffectiveTo());
        }

        for (FareSurchargeRule surcharge : surcharges) {
            addDateCut(cuts, policy, surcharge.getEffectiveFrom());
            addDateCut(cuts, policy, surcharge.getEffectiveTo());
        }

        for (LocalDate holiday : holidays) {
            addDateCut(cuts, policy, holiday);

            if (!holiday.equals(LocalDate.MAX)) {
                addDateCut(cuts, policy, holiday.plusDays(1));
            }
        }

        List<LocalDate> starts = new ArrayList<>(cuts);
        List<LocalDate> result = new ArrayList<>();

        for (int index = 0; index < starts.size(); index++) {
            LocalDate date = starts.get(index);
            LocalDate end =
                    index + 1 < starts.size() ? starts.get(index + 1) : policy.getEffectiveTo();

            // Inside an interval, rule date predicates are constant.
            // Seven consecutive dates cover every weekday.
            for (int count = 0; count < 7 && (end == null || date.isBefore(end)); count++) {

                result.add(date);

                if (date.equals(LocalDate.MAX)) {
                    break;
                }

                date = date.plusDays(1);
            }
        }

        return result;
    }

    private void addDateCut(Set<LocalDate> cuts, CinemaPricingPolicy policy, LocalDate date) {

        if (date != null
                && !date.isBefore(policy.getEffectiveFrom())
                && (policy.getEffectiveTo() == null || date.isBefore(policy.getEffectiveTo()))) {
            cuts.add(date);
        }
    }

    private ValidateDraftFareMatrixResponse response(
            CinemaFareMatrixReviewResponse review, ValidationStatus status, Report report) {

        List<String> unchecked = new ArrayList<>(review.uncheckedAreas());

        boolean matrixValid =
                status == ValidationStatus.COMPLETE
                        && !review.hasErrors()
                        && report.matrixFindingCount == 0;

        boolean unitPricingValid = matrixValid && report.surchargeFindingCount == 0;

        if (status == ValidationStatus.COMPLETE) {
            unchecked.remove("FARE_MATRIX_FULL_COVERAGE");
            unchecked.remove("HIGHEST_PRIORITY_AMBIGUITIES");
        }

        if (unitPricingValid) {
            unchecked.remove("SURCHARGE_INCLUSION_AND_STACKING");
            unchecked.add("FARE_SURCHARGE_INCLUSION_BUSINESS_CONFIRMATION");
        }

        unchecked.add("ELIGIBILITY_CODE_COMPATIBILITY");
        unchecked.add("MOVIE_ATTRIBUTE_CODE_COMPATIBILITY");
        unchecked.add("VALID_SCHEDULING_INSTANTS");

        return new ValidateDraftFareMatrixResponse(
                review.policy(),
                status,
                matrixValid,
                unitPricingValid,
                false,
                report.datesVisited,
                report.cellsChecked,
                report.ruleEvaluations,
                report.findingCount,
                report.findingCount > report.findings.size(),
                review.reportTruncated(),
                review.issues(),
                List.copyOf(report.findings),
                List.copyOf(unchecked));
    }

    private record Cell(PricingSaleScope scope, int fromMinute, int toMinute) {}

    private static final class Report {

        private long datesVisited;
        private long cellsChecked;
        private long ruleEvaluations;
        private long findingCount;
        private long matrixFindingCount;
        private long surchargeFindingCount;

        private final List<MatrixFinding> findings = new ArrayList<>();

        private void visitDate() {
            if (datesVisited >= MAX_DATE_VISITS) {
                throw new AnalysisLimitReached();
            }
            datesVisited++;
        }

        private void visitCell() {
            if (cellsChecked >= MAX_CELLS) {
                throw new AnalysisLimitReached();
            }
            cellsChecked++;
        }

        private void spendRules(int count) {
            if (ruleEvaluations > MAX_RULE_EVALUATIONS - count) {
                throw new AnalysisLimitReached();
            }
            ruleEvaluations += count;
        }

        private void add(
                String code, Cell cell, FareRuleEvaluationContext context, List<FareRule> winners) {

            matrixFindingCount++;

            append(
                    code,
                    cell,
                    context,
                    winners.stream().map(FareRule::getId).toList(),
                    Set.of(),
                    List.of());
        }

        private void addSurcharge(
                String code,
                Cell cell,
                FareRuleEvaluationContext context,
                FareRule fare,
                Set<String> movieCodes,
                List<UUID> surchargeIds) {

            surchargeFindingCount++;

            append(code, cell, context, List.of(fare.getId()), movieCodes, surchargeIds);
        }

        private void append(
                String code,
                Cell cell,
                FareRuleEvaluationContext context,
                List<UUID> fareIds,
                Set<String> movieCodes,
                List<UUID> surchargeIds) {

            findingCount++;

            if (findings.size() >= MAX_FINDINGS) {
                return;
            }

            findings.add(
                    new MatrixFinding(
                            code,
                            cell.scope().getId(),
                            cell.scope().getCode(),
                            context.localDate(),
                            context.holiday(),
                            cell.fromMinute(),
                            cell.toMinute(),
                            context.advanceScreening(),
                            context.eligibilityCodes().stream().sorted().toList(),
                            fareIds,
                            movieCodes.stream().sorted().toList(),
                            List.copyOf(surchargeIds)));
        }
    }

    private static final class AnalysisLimitReached extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
