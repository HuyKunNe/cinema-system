package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CheckDraftBasePriceCoverageRequest;
import com.cinema.inventory.dto.response.DraftBasePriceCoverageResponse;
import com.cinema.inventory.dto.response.DraftBasePriceCoverageResponse.RuleReference;
import com.cinema.inventory.dto.response.DraftBasePriceCoverageResponse.Segment;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.TicketPriceRule;
import com.cinema.inventory.enums.PricingDayType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaPricingPolicyMapper;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.TicketPriceRuleRepository;
import com.cinema.inventory.service.pricing.BasePriceRuleSelector.Status;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class DraftBasePriceCoverageService {

    private static final int MAX_RETURNED_SEGMENTS = 200;

    private final CinemaPricingPolicyRepository policyRepository;
    private final TicketPriceRuleRepository ruleRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final CinemaPricingPolicyMapper policyMapper;
    private final BasePriceRuleSelector ruleSelector;

    public DraftBasePriceCoverageService(
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
    public DraftBasePriceCoverageResponse check(
            UUID cinemaId,
            UUID policyId,
            CheckDraftBasePriceCoverageRequest request) {

        CinemaPricingPolicy policy =
                policyRepository.findForContentRead(cinemaId, policyId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requireDraft();

        if (!request.expectedVersion().equals(policy.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_VERSION_CONFLICT);
        }

        BasePriceEvaluationContext context =
                new BasePriceEvaluationContext(
                        request.startsAt(),
                        request.ticketCategory(),
                        request.simulatedMemberTiers(),
                        request.simulatedChannel());

        LocalDate localDate;

        try {
            localDate =
                    context.startsAt()
                            .toInstant()
                            .atZone(ZoneId.of(policy.getTimeZone()))
                            .toLocalDate();
        } catch (DateTimeException exception) {
            throw new ValidationException(
                    InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        if (localDate.isBefore(policy.getEffectiveFrom())
                || (policy.getEffectiveTo() != null
                        && !localDate.isBefore(policy.getEffectiveTo()))) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_POLICY_NOT_EFFECTIVE);
        }

        boolean holiday =
                holidayRepository
                        .findAllByPolicy_IdOrderByHolidayDateAsc(policyId)
                        .stream()
                        .anyMatch(entry -> entry.getHolidayDate().equals(localDate));

        PricingDayType dayType =
                holiday ? PricingDayType.HOLIDAY : PricingDayType.REGULAR;

        List<TicketPriceRule> rules =
                ruleRepository
                        .findAllByPolicy_IdOrderByPriorityDescCodeAscTicketCategoryAsc(
                                policyId);

        TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(request.fromMinute());
        boundaries.add(request.toMinute());

        for (TicketPriceRule rule : rules) {
            addBoundary(boundaries, rule.getFromMinute(), request);
            addBoundary(boundaries, rule.getToMinute(), request);
        }

        List<Integer> points = new ArrayList<>(boundaries);
        List<Segment> segments = new ArrayList<>();

        for (int index = 0; index < points.size() - 1; index++) {
            int from = points.get(index);
            int to = points.get(index + 1);

            BasePriceRuleSelector.Selection selection =
                    ruleSelector.select(
                            rules,
                            context,
                            localDate,
                            from,
                            dayType);

            List<RuleReference> references =
                    selection.winners().stream()
                            .map(
                                    rule -> new RuleReference(
                                            rule.getId(),
                                            rule.getCode(),
                                            rule.getPriority(),
                                            rule.getAmount()))
                            .toList();

            Segment next =
                    new Segment(
                            from,
                            to,
                            selection.status(),
                            selection.status() == Status.MATCHED
                                    ? selection.winners().getFirst().getAmount()
                                    : null,
                            references);

            appendOrMerge(segments, next);
        }

        boolean hasGaps =
                segments.stream().anyMatch(segment -> segment.status() == Status.GAP);

        boolean hasAmbiguities =
                segments.stream()
                        .anyMatch(segment -> segment.status() == Status.AMBIGUOUS);

        return new DraftBasePriceCoverageResponse(
                policyMapper.toResponse(policy),
                true,
                false,
                localDate,
                dayType,
                context.ticketCategory(),
                request.fromMinute(),
                request.toMinute(),
                hasGaps,
                hasAmbiguities,
                segments.size() > MAX_RETURNED_SEGMENTS,
                segments.stream().limit(MAX_RETURNED_SEGMENTS).toList());
    }

    private void addBoundary(
            TreeSet<Integer> boundaries,
            int minute,
            CheckDraftBasePriceCoverageRequest request) {

        if (minute > request.fromMinute() && minute < request.toMinute()) {
            boundaries.add(minute);
        }
    }

    private void appendOrMerge(List<Segment> segments, Segment next) {
        if (!segments.isEmpty()) {
            Segment previous = segments.getLast();

            if (previous.toMinute() == next.fromMinute()
                    && previous.status() == next.status()
                    && Objects.equals(previous.baseAmount(), next.baseAmount())
                    && previous.winningRules().equals(next.winningRules())) {

                segments.set(
                        segments.size() - 1,
                        new Segment(
                                previous.fromMinute(),
                                next.toMinute(),
                                next.status(),
                                next.baseAmount(),
                                next.winningRules()));

                return;
            }
        }

        segments.add(next);
    }
}
