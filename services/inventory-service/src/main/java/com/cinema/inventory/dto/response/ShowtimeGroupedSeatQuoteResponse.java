package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShowtimeGroupedSeatQuoteResponse(
        UUID showtimeId,
        UUID pricingPolicyVersionId,
        UUID timeBandId,
        String currency,
        OffsetDateTime serverTime,
        int seatCount,
        int capacity,
        BigDecimal totalAmount,
        List<SeatQuote> seats) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SeatQuote(
            UUID showSeatId,
            String seatNumber,
            SeatType seatType,
            int capacity,
            UUID ticketGroupId,
            String ticketGroupName,
            String eligibilityDescription,
            FareUnit unit,
                    BigDecimal price) {}
}
