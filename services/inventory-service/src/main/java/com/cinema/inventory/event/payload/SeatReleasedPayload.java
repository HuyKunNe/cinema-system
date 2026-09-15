package com.cinema.inventory.event.payload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SeatReleasedPayload(
        UUID bookingId,
        UUID showtimeId,
        List<UUID> releasedSeatIds,
        String reason,
        OffsetDateTime releasedAt) {

    public SeatReleasedPayload {

        releasedSeatIds = releasedSeatIds == null ? List.of() : List.copyOf(releasedSeatIds);
    }
}
