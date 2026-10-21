package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class FareRuleSelector {

    public Selection select(
            List<FareRule> rules,
            FareRuleEvaluationContext context) {

        if (rules == null || context == null) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        int highestPriority = Integer.MIN_VALUE;
        List<FareRule> winners = new ArrayList<>();

        for (FareRule rule : rules) {
            if (!matches(rule, context)) {
                continue;
            }

            if (rule.getPriority() > highestPriority) {
                highestPriority = rule.getPriority();
                winners.clear();
                winners.add(rule);
            } else if (rule.getPriority() == highestPriority) {
                winners.add(rule);
            }
        }

        // Sorting affects presentation only; every tied winner is retained.
        return new Selection(
                winners.stream()
                        .sorted(Comparator.comparing(FareRule::getCode))
                        .toList());
    }

    private boolean matches(
            FareRule rule,
            FareRuleEvaluationContext context) {

        if (!rule.getPolicy().getId().equals(context.policyId())
                || rule.getExperienceType() != context.experienceType()
                || rule.getContentFormat() != context.contentFormat()
                || !rule.getTicketCategoryCode().equals(context.ticketCategoryCode())
                || !rule.getSeatTypeCode().equals(context.seatTypeCode())) {
            return false;
        }

        LocalDate localDate = context.localDate();

        if (localDate.isBefore(rule.getEffectiveFrom())
                || (rule.getEffectiveTo() != null
                        && !localDate.isBefore(rule.getEffectiveTo()))) {
            return false;
        }

        if (!rule.getDayGroup().matches(localDate, context.holiday())
                || !rule.getTimeBand().matches(context.minuteOfDay())) {
            return false;
        }

        boolean firstShowingMatches =
                switch (rule.getFirstShowingCondition()) {
                    case ANY -> true;
                    case FIRST_SHOWING -> context.firstShowing();
                    case NOT_FIRST_SHOWING -> !context.firstShowing();
                };

        if (!firstShowingMatches) {
            return false;
        }

        String requiredEligibility = rule.getRequiredEligibilityCode();

        return requiredEligibility == null
                || context.eligibilityCodes().contains(requiredEligibility);
    }

    public record Selection(List<FareRule> winners) {

        public Selection {
            winners = List.copyOf(winners);
        }

        public FareRuleSelectionStatus status() {
            if (winners.isEmpty()) {
                return FareRuleSelectionStatus.GAP;
            }

            return winners.size() == 1
                    ? FareRuleSelectionStatus.MATCHED
                    : FareRuleSelectionStatus.AMBIGUOUS;
        }
    }
}
