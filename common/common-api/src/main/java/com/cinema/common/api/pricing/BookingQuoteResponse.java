package com.cinema.common.api.pricing;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record BookingQuoteResponse(
        UUID quoteId,
        UUID showtimeId,
        Long showtimeVersion,
        UUID pricingPolicyId,
        long pricingPolicyVersion,
        String timeZone,
        OffsetDateTime localStartsAt,
        LocalDate officialReleaseDate,
        boolean advanceScreening,
        String experienceType,
        String contentFormat,
        String currency,
        List<Line> items,
        String totalAmountVnd,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        boolean saleOpen,
        boolean seatsHeld) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Line(
            UUID showSeatId,
            UUID seatId,
            String seatNumber,
            String seatType,
            int capacity,
            String requestedTicketCategoryCode,
            String effectiveTicketCategoryCode,
            String effectiveTicketCategoryName,
            String categoryAdjustmentReason,
            UUID fareRuleId,
            String fareRuleCode,
            String fareUnit,
            String amountVnd,
            List<EligibilityEvidence> eligibilityEvidence) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EligibilityEvidence(
            String ruleCode,
            String reference,
            OffsetDateTime checkedAt,
            OffsetDateTime validUntil) {}
}
