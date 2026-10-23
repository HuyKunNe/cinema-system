package com.cinema.inventory.service.pricing;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreateTicketEligibilityApprovalRequest;
import com.cinema.inventory.dto.response.TicketEligibilityApprovalResponse;
import com.cinema.inventory.entity.PricingTicketCategory;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.TicketEligibilityApproval;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.enums.TicketPricingRole;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.PricingTicketCategoryRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.repository.TicketEligibilityApprovalRepository;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ManualTicketEligibilityService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PricingTicketCategoryRepository categoryRepository;
    private final TicketEligibilityApprovalRepository approvalRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public ManualTicketEligibilityService(
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            PricingTicketCategoryRepository categoryRepository,
            TicketEligibilityApprovalRepository approvalRepository,
            EntityManager entityManager,
            Clock clock) {

        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.categoryRepository = categoryRepository;
        this.approvalRepository = approvalRepository;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public TicketEligibilityApprovalResponse create(
            UUID staffId,
            CreateTicketEligibilityApprovalRequest request) {

        if (staffId == null
                || request == null
                || request.userId() == null
                || request.showtimeId() == null
                || request.showSeatId() == null
                || request.ticketCategoryCode() == null
                || !request.ticketCategoryCode()
                        .matches("[A-Z][A-Z0-9_]{0,63}")
                || !validText(request.attendeeName(), 150)
                || !validText(request.verificationReference(), 120)
                || request.validUntil() == null) {
            throw invalidApproval();
        }

        Showtime showtime =
                showtimeRepository
                        .findByIdForQuote(request.showtimeId())
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.SHOWTIME_NOT_FOUND));

        OffsetDateTime now =
                OffsetDateTime.now(clock)
                        .truncatedTo(ChronoUnit.MICROS);

        if (!showtime.usesPolicyPricing()
                || (showtime.getStatus() != ShowtimeStatus.SCHEDULED
                        && showtime.getStatus()
                                != ShowtimeStatus.OPEN_FOR_BOOKING)
                || !showtime.getStartsAt().isAfter(now)
                || !showtime.getRoom().isActive()
                || !showtime.getRoom().getCinema().isActive()) {

            throw new ConflictException(
                    InventoryErrorCode.QUOTE_SHOWTIME_UNAVAILABLE);
        }

        var policy = showtime.getPricingPolicy();
        policy.requirePublishedConfiguration();

        if (showtime.getPricingPolicyVersion() == null
                || showtime.getPricingPolicyVersion().longValue()
                        != policy.getPolicyVersion()) {

            throw new ConflictException(
                    InventoryErrorCode.SHOWTIME_POLICY_BINDING_INVALID);
        }

        var seats =
                showSeatRepository.findAllForQuote(
                        showtime.getId(),
                        Set.of(request.showSeatId()));

        if (seats.size() != 1) {
            throw new ConflictException(
                    InventoryErrorCode.QUOTE_SEAT_UNAVAILABLE);
        }

        ShowSeat seat = seats.getFirst();

        if (seat.getStatus() != ShowSeatStatus.AVAILABLE) {
            throw new ConflictException(
                    InventoryErrorCode.QUOTE_SEAT_UNAVAILABLE);
        }

        if (seat.getPrice() != null) {
            throw new ConflictException(
                    InventoryErrorCode.SHOWTIME_POLICY_PRICE_NOT_ALLOWED);
        }

        PricingTicketCategory category =
                categoryRepository
                        .findByPolicy_IdAndCode(
                                policy.getId(),
                                request.ticketCategoryCode())
                        .orElseThrow(
                                () -> new ConflictException(
                                        InventoryErrorCode
                                                .PRICING_TICKET_CATEGORY_NOT_CONFIGURED));

        // Manual approval in this round covers attendee categories only.
        if (category.getPricingRole()
                != TicketPricingRole.CATEGORY_DISCOUNT) {
            throw invalidApproval();
        }

        CategoryDiscountEligibilityGuard.requireAllowed(
                category,
                seat.getSeatType());

        OffsetDateTime validUntil =
                request.validUntil()
                        .truncatedTo(ChronoUnit.MICROS);

        if (!validUntil.isAfter(now)
                || validUntil.isAfter(showtime.getStartsAt())) {
            throw invalidApproval();
        }

        TicketEligibilityApproval approval =
                new TicketEligibilityApproval(
                        request.userId(),
                        seat,
                        category.getCode(),
                        category.getEligibilityRuleCode(),
                        request.attendeeName().trim(),
                        request.verificationReference().trim(),
                        staffId,
                        now,
                        validUntil);

        entityManager.persist(approval);
        entityManager.flush();

        return toResponse(approval);
    }

    @Transactional(readOnly = true)
    public TicketEligibilityApprovalResponse get(
            UUID userId,
            UUID approvalId) {

        TicketEligibilityApproval approval =
                approvalRepository
                        .findByIdAndUserId(approvalId, userId)
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode
                                                .ELIGIBILITY_APPROVAL_NOT_FOUND));

        return toResponse(approval);
    }

    @Transactional(readOnly = true)
    public EligibilityRule.Decision verify(
            String code,
            EligibilityRule.Context context) {

        if (context.eligibilityApprovalId() == null
                || context.ticketCategoryCode() == null
                || context.capacity() != 1) {
            throw unavailable();
        }

        var found =
                approvalRepository.findByIdAndUserId(
                        context.eligibilityApprovalId(),
                        context.userId());

        if (found.isEmpty()) {
            return rejected();
        }

        TicketEligibilityApproval approval = found.get();
        ShowSeat seat = approval.getShowSeat();

        if (!Objects.equals(
                        seat.getId(),
                        context.showSeatId())
                || !Objects.equals(
                        seat.getShowtime().getId(),
                        context.showtimeId())
                || !Objects.equals(
                        approval.getTicketCategoryCode(),
                        context.ticketCategoryCode())
                || approval.getVerifiedAt()
                        .isAfter(context.checkedAt())
                || !approval.getValidUntil()
                        .isAfter(context.checkedAt())) {

            return rejected();
        }

        // This approval proves only the category's configured condition.
        // It must not silently approve unrelated fare/program conditions.
        if (!Objects.equals(
                approval.getEligibilityRuleCode(),
                code)) {
            throw unavailable();
        }

        return new EligibilityRule.Decision(
                true,
                "MANUAL_APPROVAL:" + approval.getId(),
                approval.getValidUntil());
    }

    private static TicketEligibilityApprovalResponse toResponse(
            TicketEligibilityApproval approval) {

        return new TicketEligibilityApprovalResponse(
                approval.getId(),
                approval.getShowSeat().getShowtime().getId(),
                approval.getShowSeat().getId(),
                approval.getTicketCategoryCode(),
                approval.getAttendeeName(),
                approval.getValidUntil());
    }

    private static boolean validText(String value, int maxLength) {
        return value != null
                && !value.isBlank()
                && value.length() <= maxLength;
    }

    private static EligibilityRule.Decision rejected() {
        return new EligibilityRule.Decision(false, null, null);
    }

    private static ValidationException invalidApproval() {
        return new ValidationException(
                InventoryErrorCode.ELIGIBILITY_APPROVAL_INVALID);
    }

    private static ConflictException unavailable() {
        return new ConflictException(
                InventoryErrorCode.QUOTE_ELIGIBILITY_UNAVAILABLE);
    }
}
