package com.cinema.inventory.event.payload;

import com.cinema.common.api.pricing.BookingQuoteResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PolicySeatReservedPayload(
        UUID bookingId,
        UUID showtimeId,
        List<ReservedSeatPayload> seats,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime heldAt,
        OffsetDateTime holdExpiresAt,
        BookingQuoteResponse pricingQuote) {

    public PolicySeatReservedPayload {
        seats = List.copyOf(seats);
    }
}
