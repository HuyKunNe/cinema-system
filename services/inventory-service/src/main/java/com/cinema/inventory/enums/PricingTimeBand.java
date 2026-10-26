package com.cinema.inventory.enums;

import java.time.LocalTime;
import java.util.Objects;

public enum PricingTimeBand {
    BEFORE_17,
    FROM_17;

    public static PricingTimeBand fromLocalTime(LocalTime time) {
        Objects.requireNonNull(time, "Local showtime start is required");

        return time.isBefore(LocalTime.of(17, 0))
                ? BEFORE_17
                : FROM_17;
    }
}
