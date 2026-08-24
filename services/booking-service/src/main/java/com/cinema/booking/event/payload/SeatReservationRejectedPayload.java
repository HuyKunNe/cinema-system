package com.cinema.booking.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReservationRejectedPayload(
        UUID bookingId,
        UUID showtimeId,
        String reasonCode,
        String message,
        List<String> unavailableSeats,
        OffsetDateTime rejectedAt) {

    public SeatReservationRejectedPayload {

        unavailableSeats = unavailableSeats == null ? List.of() : List.copyOf(unavailableSeats);
    }
}
