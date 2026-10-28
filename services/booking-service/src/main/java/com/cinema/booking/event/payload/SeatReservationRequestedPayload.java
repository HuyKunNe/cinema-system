package com.cinema.booking.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReservationRequestedPayload(
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        List<RequestedSeatPayload> seats,
        OffsetDateTime requestedAt,
        OffsetDateTime holdExpiresAt,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal expectedTotalAmount) {

    public SeatReservationRequestedPayload {

        seats = List.copyOf(seats);
    }

    public SeatReservationRequestedPayload(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            List<RequestedSeatPayload> seats,
            OffsetDateTime requestedAt,
            OffsetDateTime holdExpiresAt) {

        this(bookingId, userId, showtimeId, seats, requestedAt, holdExpiresAt, null);
    }
}
