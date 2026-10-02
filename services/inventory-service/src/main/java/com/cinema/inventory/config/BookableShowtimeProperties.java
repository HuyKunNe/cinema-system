package com.cinema.inventory.config;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Component
@Validated
@ConfigurationProperties(prefix = "cinema.inventory.bookable")
public class BookableShowtimeProperties {

    private Duration maximumRange = Duration.ofDays(7);

    public Duration getMaximumRange() {
        return maximumRange;
    }

    public void setMaximumRange(Duration maximumRange) {
        this.maximumRange = maximumRange;
    }

    @AssertTrue(message = "maximum-range must be positive when configured")
    public boolean isMaximumRangeValid() {
        return maximumRange == null || (!maximumRange.isZero() && !maximumRange.isNegative());
    }
}
