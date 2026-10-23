package com.cinema.inventory.event.payload;

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

        seats = seats == null ? List.of() : List.copyOf(seats);
    }
}
