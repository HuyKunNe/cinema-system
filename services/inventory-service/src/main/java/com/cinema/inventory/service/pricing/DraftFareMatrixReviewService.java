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
import com.cinema.inventory.entity.PricingDayGroup;
import com.cinema.inventory.entity.PricingTimeBand;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.PricingDayGroupRepository;
import com.cinema.inventory.repository.PricingTimeBandRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DraftFareMatrixReviewService {

    private static final int MAX_ISSUES = 200;

    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingDayGroupRepository dayGroupRepository;
    private final PricingTimeBandRepository timeBandRepository;
    private final FareRuleRepository fareRuleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaPricingPolicyMapper policyMapper;

    public DraftFareMatrixReviewService(
            CinemaPricingPolicyRepository policyRepository,
            PricingDayGroupRepository dayGroupRepository,
            PricingTimeBandRepository timeBandRepository,
            FareRuleRepository fareRuleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaPricingPolicyMapper policyMapper) {

        this.policyRepository = policyRepository;
        this.dayGroupRepository = dayGroupRepository;
        this.timeBandRepository = timeBandRepository;
        this.fareRuleRepository = fareRuleRepository;
        this.holidayRepository = holidayRepository;
        this.policyMapper = policyMapper;
    }

    @Transactional
    public CinemaFareMatrixReviewResponse review(
            UUID cinemaId,
            UUID policyId,
            ReviewCinemaPricingPolicyRequest request) {

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
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        UUID scopedPolicyId = policy.getId();

        List<PricingDayGroup> dayGroups =
                dayGroupRepository.findAllByPolicy_IdOrderByCodeAsc(scopedPolicyId);

        List<PricingTimeBand> timeBands =
                timeBandRepository.findAllByPolicy_IdOrderByFromMinuteAscCodeAsc(
                        scopedPolicyId);

        List<FareRule> fareRules =
                fareRuleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(
                        scopedPolicyId);

        List<CinemaHolidayDate> holidays =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(
                        scopedPolicyId);

        IssueCollector report = new IssueCollector();

        checkRequiredData(policy, dayGroups, timeBands, fareRules, report);
        checkPeriodsAndUnits(policy, fareRules, holidays, report);
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
                        "TICKET_CATEGORY_CATALOG_AND_ELIGIBILITY",
                        "SEAT_TYPE_MAPPING_AND_PAIR_GROUPING",
                        "EXPERIENCE_AND_CONTENT_COMPATIBILITY",
                        "FIRST_SHOWING_DEFINITION",
                        "HOLIDAY_CALENDAR_COMPLETENESS",
                        "SURCHARGE_INCLUSION_AND_STACKING",
                        "PUBLISHED_POLICY_PERIOD_CONFLICTS",
                        "REFERENCE_TARIFF_BUSINESS_CONFIRMATION"));
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

            if (periodOutsidePolicy(
                    policy, rule.getEffectiveFrom(), rule.getEffectiveTo())) {

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

    private boolean dateOutsidePolicy(
            CinemaPricingPolicy policy,
            LocalDate date) {

        return date.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !date.isBefore(policy.getEffectiveTo()));
    }

    private boolean periodOutsidePolicy(
            CinemaPricingPolicy policy,
            LocalDate from,
            LocalDate to) {

        if (from.isBefore(policy.getEffectiveFrom())
                || (to != null && !to.isAfter(from))) {
            return true;
        }

        LocalDate policyEnd = policy.getEffectiveTo();

        return policyEnd != null
                && (!from.isBefore(policyEnd)
                        || to == null
                        || to.isAfter(policyEnd));
    }

    private static final class IssueCollector {

        private final List<MatrixReviewIssue> issues = new ArrayList<>();
        private boolean hasErrors;
        private boolean truncated;

        private void add(
                MatrixReviewSeverity severity,
                String code,
                String message,
                UUID... relatedIds) {

            // Keep the error flag accurate even after the response limit.
            if (severity == MatrixReviewSeverity.ERROR) {
                hasErrors = true;
            }

            if (issues.size() >= MAX_ISSUES) {
                truncated = true;
                return;
            }

            issues.add(
                    new MatrixReviewIssue(
                            severity,
                            code,
                            message,
                            List.of(relatedIds)));
        }
    }
}
