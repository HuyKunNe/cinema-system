package com.cinema.inventory.service.pricing;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface EligibilityRule {

    String code();

    default boolean supportsCapacity(int capacity) {
        return capacity == 1;
    }

    Decision verify(Context context);

    record Context(
            UUID userId,
            UUID showtimeId,
            UUID showSeatId,
            int capacity,
            OffsetDateTime localStartsAt,
            OffsetDateTime checkedAt) {}

    record Decision(boolean eligible, String reference, OffsetDateTime validUntil) {}
}
