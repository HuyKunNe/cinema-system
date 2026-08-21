package com.cinema.inventory.event.payload;

import com.cinema.inventory.event.SeatReservationRejectionReason;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReservationRejectedPayload(
        UUID bookingId,
        UUID showtimeId,
        SeatReservationRejectionReason reasonCode,
        String message,
        List<String> unavailableSeats,
        OffsetDateTime rejectedAt) {

    public SeatReservationRejectedPayload {

        unavailableSeats = unavailableSeats == null ? List.of() : List.copyOf(unavailableSeats);
    }
}
