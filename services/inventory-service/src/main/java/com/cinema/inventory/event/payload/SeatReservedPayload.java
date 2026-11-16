package com.cinema.inventory.event.payload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReservedPayload(
        UUID bookingId,
        UUID showtimeId,
        List<ReservedSeatPayload> seats,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime heldAt,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime showtimeEndsAt) {

    public SeatReservedPayload(
            UUID bookingId,
            UUID showtimeId,
            List<ReservedSeatPayload> seats,
            BigDecimal totalAmount,
            String currency,
            OffsetDateTime heldAt,
            OffsetDateTime holdExpiresAt) {
        this(bookingId, showtimeId, seats, totalAmount, currency, heldAt, holdExpiresAt, null);
    }

    public SeatReservedPayload {

        seats = seats == null ? List.of() : List.copyOf(seats);
    }
}
