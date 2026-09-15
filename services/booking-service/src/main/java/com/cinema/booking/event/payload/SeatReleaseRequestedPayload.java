package com.cinema.booking.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReleaseRequestedPayload(
        UUID bookingId,
        UUID showtimeId,
        List<UUID> seatIds,
        String reason,
        OffsetDateTime requestedAt) {

    public SeatReleaseRequestedPayload {

        seatIds = seatIds == null ? List.of() : List.copyOf(seatIds);
    }
}
