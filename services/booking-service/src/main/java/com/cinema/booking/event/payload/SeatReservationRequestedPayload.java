package com.cinema.booking.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReservationRequestedPayload(
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        List<RequestedSeatPayload> seats,
        OffsetDateTime requestedAt,
        OffsetDateTime holdExpiresAt) {

    public SeatReservationRequestedPayload {

        seats = List.copyOf(seats);
    }
}
