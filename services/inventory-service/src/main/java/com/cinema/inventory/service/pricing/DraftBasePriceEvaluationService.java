package com.cinema.inventory.service.pricing;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class DraftBasePriceEvaluationService {

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketPriceRuleRepository ruleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaPricingPolicyMapper policyMapper;
    private final BasePriceRuleSelector ruleSelector;

    public DraftBasePriceEvaluationService(
            CinemaPricingPolicyRepository policyRepository,
            TicketPriceRuleRepository ruleRepository,
            CinemaHolidayDateRepository holidayRepository,
            CinemaPricingPolicyMapper policyMapper,
            BasePriceRuleSelector ruleSelector) {

        this.policyRepository = policyRepository;
        this.ruleRepository = ruleRepository;
        this.holidayRepository = holidayRepository;
        this.policyMapper = policyMapper;
        this.ruleSelector = ruleSelector;
    }

    @Transactional
    public BasePriceEvaluationResult evaluate(
            UUID cinemaId, UUID policyId, BasePriceEvaluationContext context) {

        if (context == null) {
            throw new ValidationException(InventoryErrorCode.PRICING_EVALUATION_CONTEXT_INVALID);
        }

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireDraft();

        OffsetDateTime localStartsAt = toLocalStartsAt(policy, context);
        LocalDate localDate = localStartsAt.toLocalDate();

        requireEffectiveDate(policy, localDate);

        boolean holiday =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId()).stream()
                        .anyMatch(entry -> entry.getHolidayDate().equals(localDate));

        PricingDayType dayType = holiday ? PricingDayType.HOLIDAY : PricingDayType.REGULAR;

        int minuteOfDay = localStartsAt.getHour() * 60 + localStartsAt.getMinute();

        BasePriceRuleSelector.Selection selection =
                ruleSelector.select(
                        ruleRepository
                                .findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                                        policy.getId()),
                        context,
                        localDate,
                        minuteOfDay,
                        dayType);

        if (selection.status() == BasePriceRuleSelector.Status.GAP) {
            throw new ConflictException(InventoryErrorCode.TICKET_PRICE_RULE_NOT_FOUND);
        }

        if (selection.status() == BasePriceRuleSelector.Status.AMBIGUOUS) {
            throw new ConflictException(InventoryErrorCode.TICKET_PRICE_RULE_AMBIGUOUS);
        }

        TicketPriceRule winner = selection.winners().getFirst();

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
    private OffsetDateTime toLocalStartsAt(
            CinemaPricingPolicy policy, BasePriceEvaluationContext context) {

        String timeZone = policy.getTimeZone();

        if (timeZone == null || timeZone.isBlank()) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        try {
            return context.startsAt().toInstant().atZone(ZoneId.of(timeZone)).toOffsetDateTime();
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
