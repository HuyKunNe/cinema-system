package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyReviewResponse;
import com.cinema.inventory.dto.response.CinemaPricingPolicyReviewResponse.Issue;
import com.cinema.inventory.dto.response.CinemaPricingPolicyReviewResponse.Severity;
import com.cinema.inventory.entity.CinemaHolidayDate;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.entity.TicketSurchargeRule;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.enums.SurchargeTarget;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.TicketPriceRuleRepository;
import com.cinema.inventory.repository.TicketSurchargeRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class DraftPricingPolicyReviewService {

    private static final int MAX_ISSUES = 200;

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketPriceRuleRepository ruleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final TicketSurchargeRuleRepository surchargeRepository;
    private final CinemaPricingPolicyMapper policyMapper;

    public DraftPricingPolicyReviewService(
            CinemaPricingPolicyRepository policyRepository,
            TicketPriceRuleRepository ruleRepository,
            CinemaHolidayDateRepository holidayRepository,
            TicketSurchargeRuleRepository surchargeRepository,
            CinemaPricingPolicyMapper policyMapper) {

        this.policyRepository = policyRepository;
        this.ruleRepository = ruleRepository;
        this.holidayRepository = holidayRepository;
        this.surchargeRepository = surchargeRepository;
        this.policyMapper = policyMapper;
    }

    @Transactional
    public CinemaPricingPolicyReviewResponse review(
            UUID cinemaId, UUID policyId, ReviewCinemaPricingPolicyRequest request) {

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));
        policy.requireBaseRulesModel();
        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        List<TicketPriceRule> rules =
                ruleRepository.findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                        policyId);

        List<CinemaHolidayDate> holidays =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policyId);

        List<TicketSurchargeRule> surcharges =
                surchargeRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policyId);

        IssueCollector report = new IssueCollector();

        checkRequiredData(policy, rules, holidays, report);
        checkCategories(rules, report);
        checkSurcharges(surcharges, report);
        checkPotentialOverlaps(rules, report);

        return new CinemaPricingPolicyReviewResponse(
                policyMapper.toResponse(policy),
                report.hasErrors,
                report.truncated,
                false,
                List.copyOf(report.issues),
                List.of(
                        "BASE_PRICE_FULL_COVERAGE",
                        "HIGHEST_PRIORITY_CONFLICT_PROOF",
                        "CUSTOMER_ELIGIBILITY",
                        "SURCHARGE_CALCULATION",
                        "RUNTIME_MOVIE_AND_FORMAT_CODES",
                        "PUBLISHED_POLICY_PERIOD_CONFLICTS",
                        "BUSINESS_RULE_CONFIRMATION"));
    }

    private void checkRequiredData(
            CinemaPricingPolicy policy,
            List<TicketPriceRule> rules,
            List<CinemaHolidayDate> holidays,
            IssueCollector report) {

        if (rules.isEmpty()) {
            report.add(
                    Severity.ERROR,
                    "BASE_RULES_EMPTY",
                    "The policy has no base ticket price rules.",
                    null);
        }

        for (CinemaHolidayDate holiday : holidays) {
            boolean outside =
                    holiday.getHolidayDate().isBefore(policy.getEffectiveFrom())
                            || (policy.getEffectiveTo() != null
                                    && !holiday.getHolidayDate().isBefore(policy.getEffectiveTo()));

            if (outside) {
                report.add(
                        Severity.ERROR,
                        "HOLIDAY_OUTSIDE_POLICY_PERIOD",
                        "The holiday date is outside the policy effective period.",
                        null,
                        holiday.getId());
            }
        }

        boolean hasHolidayRules =
                rules.stream().anyMatch(rule -> rule.getDayType() == PricingDayType.HOLIDAY);

        if (hasHolidayRules && holidays.isEmpty()) {
            report.add(
                    Severity.WARNING,
                    "HOLIDAY_RULES_WITHOUT_DATES",
                    "Holiday rules exist but the policy holiday calendar is empty.",
                    null);
        }
    }

    private void checkCategories(List<TicketPriceRule> rules, IssueCollector report) {

        for (TicketCategory category : TicketCategory.values()) {
            boolean present = rules.stream().anyMatch(rule -> rule.getTicketCategory() == category);

            if (!present) {
                report.add(
                        Severity.WARNING,
                        "TICKET_CATEGORY_WITHOUT_RULES",
                        "No base price rule exists for this ticket category.",
                        category);
            }
        }
    }

    private void checkSurcharges(List<TicketSurchargeRule> surcharges, IssueCollector report) {

        for (TicketSurchargeRule rule : surcharges) {
            if (rule.getTarget() != SurchargeTarget.SEAT_TYPE) {
                report.add(
                        Severity.WARNING,
                        "SURCHARGE_RUNTIME_CONTEXT_UNAVAILABLE",
                        "Runtime movie attribute or showtime format support is not implemented.",
                        null,
                        rule.getId());
            }

            if (!rule.isStackable()) {
                report.add(
                        Severity.WARNING,
                        "SURCHARGE_EXCLUSION_RULE_UNCONFIRMED",
                        "The selection and exclusion behavior for non-stackable surcharges "
                                + "must be confirmed before calculation.",
                        null,
                        rule.getId());
            }

            if (rule.getTarget() == SurchargeTarget.SEAT_TYPE
                    && "COUPLE".equals(rule.getTargetCode())) {
                report.add(
                        Severity.WARNING,
                        "COUPLE_PRICING_RULE_UNCONFIRMED",
                        "Confirm ticket grouping and the surcharge unit for couple seats.",
                        null,
                        rule.getId());
            }
        }
    }

    private void checkPotentialOverlaps(List<TicketPriceRule> rules, IssueCollector report) {

        for (int first = 0; first < rules.size(); first++) {
            for (int second = first + 1; second < rules.size(); second++) {
                TicketPriceRule left = rules.get(first);
                TicketPriceRule right = rules.get(second);

                if (!potentiallyOverlap(left, right)) {
                    continue;
                }

                report.add(
                        Severity.WARNING,
                        "POTENTIAL_EQUAL_PRIORITY_OVERLAP",
                        "Equal-priority rule conditions overlap. Actual calendar dates "
                                + "and higher-priority rules have not been analyzed.",
                        left.getTicketCategory(),
                        left.getId(),
                        right.getId());

                if (report.truncated) {
                    return;
                }
            }
        }
    }

    private boolean potentiallyOverlap(TicketPriceRule left, TicketPriceRule right) {

        if (left.getTicketCategory() != right.getTicketCategory()
                || left.getPriority() != right.getPriority()
                || left.getFromMinute() >= right.getToMinute()
                || right.getFromMinute() >= left.getToMinute()
                || Collections.disjoint(left.getDaysOfWeek(), right.getDaysOfWeek())) {
            return false;
        }

        boolean dayTypesCompatible =
                left.getDayType() == PricingDayType.ANY
                        || right.getDayType() == PricingDayType.ANY
                        || left.getDayType() == right.getDayType();

        if (!dayTypesCompatible) {
            return false;
        }

        String leftChannel = left.getRequiredChannel();
        String rightChannel = right.getRequiredChannel();

        // Different membership requirements can coexist in the current
        // set-based evaluation context. They are not treated as exclusive.
        return leftChannel == null || rightChannel == null || leftChannel.equals(rightChannel);
    }

    private static final class IssueCollector {

        private final List<Issue> issues = new ArrayList<>();
        private boolean hasErrors;
        private boolean truncated;

        private void add(
                Severity severity,
                String code,
                String message,
                TicketCategory category,
                UUID... relatedIds) {

            if (severity == Severity.ERROR) {
                hasErrors = true;
            }

            if (issues.size() >= MAX_ISSUES) {
                truncated = true;
                return;
            }

            issues.add(new Issue(severity, code, message, category, List.of(relatedIds)));
        }
    }
}
