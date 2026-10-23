package com.cinema.inventory.service.pricing;

import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.api.pricing.BookingQuoteResponse.EligibilityEvidence;
import com.cinema.common.api.pricing.BookingQuoteResponse.Line;
import com.cinema.common.api.pricing.CreateBookingQuoteRequest;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.FareRule;
import com.cinema.inventory.entity.PricingQuote;
import com.cinema.inventory.entity.PricingSeatType;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.FareRuleSelectionStatus;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingSeatMappingStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.enums.TicketEligibilityMode;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaHolidayDateRepository;
import com.cinema.inventory.repository.CinemaPricingPolicyRepository;
import com.cinema.inventory.repository.FareRuleRepository;
import com.cinema.inventory.repository.PricingSeatTypeRepository;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
public class BookingQuotePricingService {

    // DECIMAL(19,2) in the existing booking money snapshots:
    // at most 17 integer digits for integer VND.
    private static final long MAX_BOOKING_AMOUNT_VND = 99_999_999_999_999_999L;

    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final CinemaPricingPolicyRepository policyRepository;
    private final PricingTicketCategoryRepository categoryRepository;
    private final PricingSeatTypeRepository seatTypeRepository;
    private final CinemaHolidayDateRepository holidayRepository;
    private final FareRuleRepository fareRepository;
    private final FareCategoryResolver categoryResolver;
    private final DraftSaleScopeResolver scopeResolver;
    private final FareRuleSelector selector;
    private final EligibilityRuleRegistry eligibilityRegistry;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration ttl;
    private final int maxSeats;

    public BookingQuotePricingService(
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            CinemaPricingPolicyRepository policyRepository,
            PricingTicketCategoryRepository categoryRepository,
            PricingSeatTypeRepository seatTypeRepository,
            CinemaHolidayDateRepository holidayRepository,
            FareRuleRepository fareRepository,
            FareCategoryResolver categoryResolver,
            DraftSaleScopeResolver scopeResolver,
            FareRuleSelector selector,
            EligibilityRuleRegistry eligibilityRegistry,
            EntityManager entityManager,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${cinema.inventory.quote.ttl:PT3M}") Duration ttl,
            @Value("${cinema.inventory.quote.max-seats:10}") int maxSeats) {

        if (ttl.isZero() || ttl.isNegative() || maxSeats < 1 || maxSeats > 100) {
            throw new IllegalStateException("Invalid quote limits");
        }

        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.policyRepository = policyRepository;
        this.categoryRepository = categoryRepository;
        this.seatTypeRepository = seatTypeRepository;
        this.holidayRepository = holidayRepository;
        this.fareRepository = fareRepository;
        this.categoryResolver = categoryResolver;
        this.scopeResolver = scopeResolver;
        this.selector = selector;
        this.eligibilityRegistry = eligibilityRegistry;
        this.entityManager = entityManager;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.ttl = ttl;
        this.maxSeats = maxSeats;
    }

    @Transactional
    public BookingQuoteResponse create(UUID userId, CreateBookingQuoteRequest request) {

        if (userId == null
                || request == null
                || request.items() == null
                || request.items().isEmpty()
                || request.items().size() > maxSeats) {
            throw invalidRequest();
        }

        Set<UUID> ids = new HashSet<>();

        for (var item : request.items()) {
            if (item == null || item.showSeatId() == null || !ids.add(item.showSeatId())) {
                throw invalidRequest();
            }
        }

        Showtime showtime =
                showtimeRepository
                        .findByIdForQuote(request.showtimeId())
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        if (!showtime.usesPolicyPricing()
                || (showtime.getStatus() != ShowtimeStatus.SCHEDULED
                        && showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING)
                || !showtime.getStartsAt().isAfter(now)
                || !showtime.getRoom().isActive()
                || !showtime.getRoom().getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.QUOTE_SHOWTIME_UNAVAILABLE);
        }

        CinemaPricingPolicy policy =
                policyRepository
                        .findForContentRead(
                                showtime.getRoom().getCinema().getId(),
                                showtime.getPricingPolicy().getId())
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.PRICING_POLICY_NOT_FOUND));

        policy.requirePublishedConfiguration();

        if (showtime.getPricingPolicyVersion() == null
                || showtime.getPricingPolicyVersion().longValue() != policy.getPolicyVersion()
                || showtime.getExperienceType() == null
                || showtime.getContentFormat() == null
                || showtime.getOfficialReleaseDate() == null
                || !PricingPolicyPeriod.from(policy).contains(showtime.getStartsAt().toInstant())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
        }

        Map<UUID, ShowSeat> seats =
                showSeatRepository.findAllForQuote(showtime.getId(), ids).stream()
                        .collect(Collectors.toMap(ShowSeat::getId, Function.identity()));

        if (seats.size() != ids.size()) {
            throw seatUnavailable();
        }

        List<PricingTicketCategory> categories =
                categoryRepository.findAllByPolicy_IdOrderByCodeAsc(policy.getId());

        Map<String, PricingTicketCategory> categoriesByCode =
                categories.stream()
                        .collect(
                                Collectors.toMap(
                                        PricingTicketCategory::getCode, Function.identity()));

        List<PricingSeatType> seatTypes =
                seatTypeRepository.findAllByPolicy_IdOrderByCodeAsc(policy.getId());

        List<FareRule> fares =
                fareRepository.findAllByPolicy_IdOrderByPriorityDescCodeAsc(policy.getId());

        OffsetDateTime localStartsAt =
                showtime.getStartsAt()
                        .toInstant()
                        .atZone(ZoneId.of(policy.getTimeZone()))
                        .toOffsetDateTime();

        boolean holiday =
                holidayRepository.findAllByPolicy_IdOrderByHolidayDateAsc(policy.getId()).stream()
                        .anyMatch(
                                entry ->
                                        entry.getHolidayDate().equals(localStartsAt.toLocalDate()));

        OffsetDateTime expiresAt = earlier(now.plus(ttl), showtime.getStartsAt());

        List<Line> lines = new ArrayList<>();
        long total = 0;

        for (var item : request.items()) {
            PricedLine priced =
                    priceLine(
                            userId,
                            showtime,
                            policy,
                            seats.get(item.showSeatId()),
                            item.ticketCategoryCode(),
                            item.eligibilityApprovalId(),
                            localStartsAt,
                            holiday,
                            now,
                            categories,
                            categoriesByCode,
                            seatTypes,
                            fares);

            try {
                total = Math.addExact(total, priced.amount());
            } catch (ArithmeticException exception) {
                throw invalidAmount();
            }

            if (total > MAX_BOOKING_AMOUNT_VND) {
                throw invalidAmount();
            }

            lines.add(priced.line());
            expiresAt = earlier(expiresAt, priced.evidenceExpiresAt());
        }

        expiresAt = expiresAt.truncatedTo(ChronoUnit.MICROS);

        if (!expiresAt.isAfter(OffsetDateTime.now(clock))) {
            throw new ConflictException(InventoryErrorCode.QUOTE_ELIGIBILITY_UNAVAILABLE);
        }

        PricingQuote quote = new PricingQuote(userId, showtime, policy, now, expiresAt);

        BookingQuoteResponse response =
                new BookingQuoteResponse(
                        quote.getId(),
                        showtime.getId(),
                        showtime.getVersion(),
                        policy.getId(),
                        policy.getPolicyVersion(),
                        policy.getTimeZone(),
                        localStartsAt,
                        showtime.getOfficialReleaseDate(),
                        Boolean.TRUE.equals(showtime.getAdvanceScreening()),
                        objectMapper.convertValue(showtime.getExperienceType(), String.class),
                        objectMapper.convertValue(showtime.getContentFormat(), String.class),
                        policy.getCurrency(),
                        List.copyOf(lines),
                        Long.toString(total),
                        now,
                        expiresAt,
                        showtime.getStatus() == ShowtimeStatus.OPEN_FOR_BOOKING,
                        false);

        try {
            quote.sealSnapshot(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException exception) {
            throw new InternalServerException(InventoryErrorCode.QUOTE_SNAPSHOT_FAILED, exception);
        }

        entityManager.persist(quote);
        entityManager.flush();

        return response;
    }

    private PricedLine priceLine(
            UUID userId,
            Showtime showtime,
            CinemaPricingPolicy policy,
            ShowSeat seat,
            String requestedCategory,
            UUID eligibilityApprovalId,
            OffsetDateTime localStartsAt,
            boolean holiday,
            OffsetDateTime now,
            List<PricingTicketCategory> categories,
            Map<String, PricingTicketCategory> categoriesByCode,
            List<PricingSeatType> seatTypes,
            List<FareRule> fares) {

        if (seat.getStatus() != ShowSeatStatus.AVAILABLE) {
            throw seatUnavailable();
        }

        if (seat.getPrice() != null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_POLICY_PRICE_NOT_ALLOWED);
        }

        List<PricingSeatType> mappings =
                seatTypes.stream()
                        .filter(
                                type ->
                                        type.getMappingStatus()
                                                == PricingSeatMappingStatus.CONFIRMED)
                        .filter(type -> type.getPhysicalSeatType() == seat.getSeatType())
                        .toList();

        if (mappings.size() != 1) {
            throw fareUnavailable();
        }

        FareRuleEvaluationContext raw =
                new FareRuleEvaluationContext(
                        policy.getId(),
                        localStartsAt,
                        holiday,
                        showtime.getExperienceType(),
                        showtime.getContentFormat(),
                        requestedCategory,
                        mappings.getFirst().getCode(),
                        showtime.getOfficialReleaseDate(),
                        Set.of());

        // Resolve business exclusions before requesting customer evidence.
        var initialResolution = categoryResolver.resolve(raw, categories);
        var initialContext = initialResolution.context();

        PricingTicketCategory category = categoriesByCode.get(initialContext.ticketCategoryCode());

        if (category == null
                || category.getEligibilityMode() == TicketEligibilityMode.UNCONFIRMED) {
            throw new ConflictException(
                    InventoryErrorCode.PRICING_TICKET_CATEGORY_ELIGIBILITY_UNCONFIRMED);
        }

        CategoryDiscountEligibilityGuard.requireAllowed(category, seat.getSeatType());

        if (!scopeResolver.resolve(initialContext).allowsMinute(initialContext.minuteOfDay())) {
            throw new ConflictException(InventoryErrorCode.PRICING_SCENARIO_NOT_FOR_SALE);
        }

        List<FareRule> candidates =
                fares.stream()
                        .filter(
                                rule ->
                                        selector.matchesDimensions(
                                                rule, initialContext, initialContext.minuteOfDay()))
                        .toList();

        Map<String, EligibilityEvidence> verified = new HashMap<>();
        Set<String> checkedCodes = new HashSet<>();

        EligibilityRule.Context verificationContext =
                new EligibilityRule.Context(
                        userId,
                        showtime.getId(),
                        seat.getId(),
                        seat.getSeatType().getCapacity(),
                        localStartsAt,
                        now,
                        eligibilityApprovalId,
                        category.getCode());

        // Category eligibility is mandatory before selecting a fare.
        if (category.getEligibilityMode() == TicketEligibilityMode.RULE_REQUIRED) {
            String categoryCode = category.getEligibilityRuleCode();

            if (categoryCode == null || categoryCode.isBlank()) {
                throw eligibilityUnavailable();
            }

            verifyEligibility(categoryCode, verificationContext, checkedCodes, verified);

            var categoryResolution =
                    categoryResolver.resolve(withEligibility(raw, verified.keySet()), categories);

            if (!categoryResolution.context().eligibilityCodes().contains(categoryCode)) {
                throw new ConflictException(InventoryErrorCode.QUOTE_CATEGORY_NOT_ELIGIBLE);
            }
        }

        // Only verify fare conditions at priorities that can affect the result.
        FareRule fare =
                selectVerifiedFare(
                        candidates, raw, categories, verificationContext, checkedCodes, verified);

        var resolution =
                categoryResolver.resolve(withEligibility(raw, verified.keySet()), categories);

        var context = resolution.context();

        FareUnit expectedUnit =
                seat.getSeatType() == SeatType.COUPLE ? FareUnit.PER_PAIR : FareUnit.PER_TICKET;

        if (fare.getFareUnit() != expectedUnit
                || fare.getAmountVnd() < 0
                || fare.getAmountVnd() > MAX_BOOKING_AMOUNT_VND) {
            throw fareUnavailable();
        }

        Set<String> evidenceUsed = new TreeSet<>();

        if (category.getEligibilityMode() == TicketEligibilityMode.RULE_REQUIRED) {
            evidenceUsed.add(category.getEligibilityRuleCode());
        }

        if (fare.getRequiredEligibilityCode() != null) {
            evidenceUsed.add(fare.getRequiredEligibilityCode());
        }

        List<EligibilityEvidence> evidence = evidenceUsed.stream().map(verified::get).toList();

        if (evidence.stream().anyMatch(Objects::isNull)) {
            throw eligibilityUnavailable();
        }

        OffsetDateTime evidenceExpiresAt =
                evidence.stream()
                        .map(EligibilityEvidence::validUntil)
                        .filter(Objects::nonNull)
                        .min(Comparator.comparing(OffsetDateTime::toInstant))
                        .orElse(null);

        Line line =
                new Line(
                        seat.getId(),
                        seat.getSeat().getId(),
                        seat.getSeatNumber(),
                        seat.getSeatType().name(),
                        seat.getSeatType().getCapacity(),
                        resolution.requestedTicketCategoryCode(),
                        context.ticketCategoryCode(),
                        category.getName(),
                        resolution.reason().name(),
                        fare.getId(),
                        fare.getCode(),
                        fare.getFareUnit().name(),
                        Long.toString(fare.getAmountVnd()),
                        List.copyOf(evidence));

        return new PricedLine(line, fare.getAmountVnd(), evidenceExpiresAt);
    }

    private static OffsetDateTime earlier(OffsetDateTime current, OffsetDateTime candidate) {

        return candidate != null && candidate.isBefore(current) ? candidate : current;
    }

    private FareRule selectVerifiedFare(
            List<FareRule> candidates,
            FareRuleEvaluationContext raw,
            List<PricingTicketCategory> categories,
            EligibilityRule.Context verificationContext,
            Set<String> checkedCodes,
            Map<String, EligibilityEvidence> verified) {

        List<FareRule> ordered =
                candidates.stream()
                        .sorted(
                                Comparator.comparingInt(FareRule::getPriority)
                                        .reversed()
                                        .thenComparing(FareRule::getCode))
                        .toList();

        int index = 0;

        while (index < ordered.size()) {
            int start = index;
            int priority = ordered.get(index).getPriority();

            while (index < ordered.size() && ordered.get(index).getPriority() == priority) {
                index++;
            }

            List<FareRule> priorityGroup = ordered.subList(start, index);

            // Verify the whole priority group to detect tied eligible fares.
            Set<String> groupCodes =
                    priorityGroup.stream()
                            .map(FareRule::getRequiredEligibilityCode)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toCollection(TreeSet::new));

            for (String code : groupCodes) {
                verifyEligibility(code, verificationContext, checkedCodes, verified);
            }

            var resolution =
                    categoryResolver.resolve(withEligibility(raw, verified.keySet()), categories);

            var selection = selector.select(priorityGroup, resolution.context());

            if (selection.status() == FareRuleSelectionStatus.AMBIGUOUS) {
                throw fareUnavailable();
            }

            if (selection.status() == FareRuleSelectionStatus.MATCHED) {
                return selection.winners().getFirst();
            }

            // Every fare at this priority was conclusively ineligible.
            // Continue to the next lower priority.
        }

        throw fareUnavailable();
    }

    private void verifyEligibility(
            String code,
            EligibilityRule.Context context,
            Set<String> checkedCodes,
            Map<String, EligibilityEvidence> verified) {

        if (code == null || code.isBlank()) {
            throw eligibilityUnavailable();
        }

        if (checkedCodes.contains(code)) {
            return;
        }

        // Bound actual verifier calls per line, including rejected decisions.
        if (checkedCodes.size() >= 20) {
            throw eligibilityUnavailable();
        }

        var decision = eligibilityRegistry.verify(code, context);

        checkedCodes.add(code);

        if (decision.eligible()) {
            verified.put(
                    code,
                    new EligibilityEvidence(
                            code,
                            decision.reference(),
                            context.checkedAt(),
                            decision.validUntil()));
        }
    }

    private static FareRuleEvaluationContext withEligibility(
            FareRuleEvaluationContext source, Set<String> eligibilityCodes) {

        return new FareRuleEvaluationContext(
                source.policyId(),
                source.localStartsAt(),
                source.holiday(),
                source.experienceType(),
                source.contentFormat(),
                source.ticketCategoryCode(),
                source.seatTypeCode(),
                source.officialReleaseDate(),
                Set.copyOf(eligibilityCodes));
    }

    private static ValidationException invalidRequest() {
        return new ValidationException(InventoryErrorCode.QUOTE_REQUEST_INVALID);
    }

    private static ConflictException seatUnavailable() {
        return new ConflictException(InventoryErrorCode.QUOTE_SEAT_UNAVAILABLE);
    }

    private static ConflictException fareUnavailable() {
        return new ConflictException(InventoryErrorCode.QUOTE_FARE_UNAVAILABLE);
    }

    private static ConflictException eligibilityUnavailable() {
        return new ConflictException(InventoryErrorCode.QUOTE_ELIGIBILITY_UNAVAILABLE);
    }

    private static ConflictException invalidAmount() {
        return new ConflictException(InventoryErrorCode.QUOTE_AMOUNT_INVALID);
    }

    private record PricedLine(Line line, long amount, OffsetDateTime evidenceExpiresAt) {}
}
