package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ExperienceType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateShowtimeRequest(
        @NotNull UUID movieId,
        @NotNull UUID roomId,
        @NotNull @Future OffsetDateTime startsAt,
        @NotNull @Future OffsetDateTime endsAt,
        @NotNull ExperienceType experienceType,
        @NotNull ContentFormat contentFormat) {

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }
}
