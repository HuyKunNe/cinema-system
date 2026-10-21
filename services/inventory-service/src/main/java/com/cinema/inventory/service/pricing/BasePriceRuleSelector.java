package com.cinema.inventory.service.pricing;

import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.enums.PricingDayType;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class BasePriceRuleSelector {

    public enum Status {
        MATCHED,
        GAP,
        AMBIGUOUS
    }

    public record Selection(List<TicketPriceRule> winners) {

        public Selection {
            winners = List.copyOf(winners);
        }

        public Status status() {
            return switch (winners.size()) {
                case 0 -> Status.GAP;
                case 1 -> Status.MATCHED;
                default -> Status.AMBIGUOUS;
            };
        }
    }

    public Selection select(
            List<TicketPriceRule> rules,
            BasePriceEvaluationContext context,
            LocalDate localDate,
            int minuteOfDay,
            PricingDayType dayType) {

        List<TicketPriceRule> candidates =
                rules.stream()
                        .filter(rule -> matches(rule, context, localDate, minuteOfDay, dayType))
                        .toList();

        if (candidates.isEmpty()) {
            return new Selection(List.of());
        }

        int highestPriority =
                candidates.stream().mapToInt(TicketPriceRule::getPriority).max().orElse(0);

        return new Selection(
                candidates.stream().filter(rule -> rule.getPriority() == highestPriority).toList());
    }

    private boolean matches(
            TicketPriceRule rule,
            BasePriceEvaluationContext context,
            LocalDate localDate,
            int minuteOfDay,
            PricingDayType dayType) {

        if (rule.getTicketCategory() != context.ticketCategory()
                || !rule.getDaysOfWeek().contains(localDate.getDayOfWeek())
                || (rule.getDayType() != PricingDayType.ANY && rule.getDayType() != dayType)
                || minuteOfDay < rule.getFromMinute()
                || minuteOfDay >= rule.getToMinute()) {
            return false;
        }

        if (rule.getRequiredMemberTier() != null
                && !context.memberTiers().contains(rule.getRequiredMemberTier())) {
            return false;
        }

        return rule.getRequiredChannel() == null
                || rule.getRequiredChannel().equals(context.channel());
    }
}
