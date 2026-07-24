package com.cinema.inventory.dto.request;

import java.time.OffsetDateTime;

import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record UpdateShowtimeRequest(
        @NotNull OffsetDateTime startsAt,

        @NotNull OffsetDateTime endsAt,

        @NotNull ShowtimeStatus status) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null
                || endsAt == null
                || endsAt.isAfter(startsAt);
    }
}