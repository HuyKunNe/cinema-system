package com.cinema.inventory.dto.request;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateShowtimeRequest(
        @NotNull
        UUID movieId,

        @NotNull
        UUID roomId,

        @NotNull
        @Future
        OffsetDateTime startsAt,

        @NotNull
        @Future
        OffsetDateTime endsAt) {

    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null
                || endsAt == null
                || endsAt.isAfter(startsAt);
    }
}