package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.TicketPriceRuleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class DraftBasePriceEvaluationService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketPriceRuleRepository ruleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaPricingPolicyMapper policyMapper;

    public DraftBasePriceEvaluationService(
            CinemaPricingPolicyRepository policyRepository,
            TicketPriceRuleRepository ruleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaPricingPolicyMapper policyMapper) {

        this.policyRepository = policyRepository;
        this.ruleRepository = ruleRepository;
        this.holidayRepository = holidayRepository;
        this.policyMapper = policyMapper;
    }

    @Transactional
    public BasePriceEvaluationResult evaluate(
            UUID cinemaId,
            UUID policyId,
            BasePriceEvaluationContext context) {

        if (context == null) {
            throw new ValidationException(
                    InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireDraft();

        OffsetDateTime localStartsAt = toLocalStartsAt(policy, context);
        LocalDate localDate = localStartsAt.toLocalDate();

        requireEffectiveDate(policy, localDate);

        boolean holiday =
                holidayRepository
                        .findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId())
                        .stream()
                        .anyMatch(
                                entry -> entry.getHolidayDate().equals(localDate));

        PricingDayType dayType =
                holiday ? PricingDayType.HOLIDAY : PricingDayType.REGULAR;

        int minuteOfDay =
                localStartsAt.getHour() * 60 + localStartsAt.getMinute();

        List<TicketPriceRule> candidates =
                ruleRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                                policy.getId())
                        .stream()
                        .filter(
                                rule ->
                                        matches(
                                                rule,
                                                context,
                                                localDate,
                                                minuteOfDay,
                                                dayType))
                        .toList();

        if (candidates.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.TICKET_PRICE_RULE_NOT_FOUND);
        }

        int highestPriority =
                candidates.stream()
                        .mapToInt(TicketPriceRule::getPriority)
                        .max()
                        .orElseThrow(
                                () -> new ConflictException(
                                        InventoryErrorCode.TICKET_PRICE_RULE_NOT_FOUND));

        List<TicketPriceRule> winners =
                candidates.stream()
                        .filter(rule -> rule.getPriority() == highestPriority)
                        .toList();

        if (winners.size() != 1) {
            throw new ConflictException(
                    InventoryErrorCode.TICKET_PRICE_RULE_AMBIGUOUS);
        }

        TicketPriceRule winner = winners.getFirst();

        return new BasePriceEvaluationResult(
                policyMapper.toResponse(policy),
                winner.getId(),
                winner.getCode(),
                context.ticketCategory(),
                localStartsAt,
                dayType,
                winner.getPriority(),
                winner.getAmount());
    }

    private boolean matches(
            TicketPriceRule rule,
            BasePriceEvaluationContext context,
            LocalDate localDate,
            int minuteOfDay,
            PricingDayType dayType) {

        if (rule.getTicketCategory() != context.ticketCategory()
                || !rule.getDaysOfWeek().contains(localDate.getDayOfWeek())
                || (rule.getDayType() != PricingDayType.ANY
                        && rule.getDayType() != dayType)
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

    private OffsetDateTime toLocalStartsAt(
            CinemaPricingPolicy policy,
            BasePriceEvaluationContext context) {

        String timeZone = policy.getTimeZone();

        if (timeZone == null || timeZone.isBlank()) {
            throw new ValidationException(
                    InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        try {
            return context.startsAt()
                    .toInstant()
                    .atZone(ZoneId.of(timeZone))
                    .toOffsetDateTime();
        } catch (DateTimeException exception) {
            throw new ValidationException(
                    InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }
    }

    private void requireEffectiveDate(
            CinemaPricingPolicy policy,
            LocalDate localDate) {

        if (localDate.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !localDate.isBefore(policy.getEffectiveTo()))) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }
    }
}
