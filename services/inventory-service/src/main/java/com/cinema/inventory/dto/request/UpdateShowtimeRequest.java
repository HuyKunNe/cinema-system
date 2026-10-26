package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ShowtimeStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record UpdateShowtimeRequest(
        @NotNull @Future OffsetDateTime startsAt,
        @NotNull @Future OffsetDateTime endsAt,
        @Schema(
                        deprecated = true,
                        description =
                                "Optional compatibility field. "
                                        + "Only SCHEDULED is accepted. "
                                        + "Use lifecycle PATCH endpoints to change status.")
                ShowtimeStatus status,
        @NotNull @Min(0) Long expectedVersion,
        @NotNull @Min(0) @Max(120) Integer breakMinutes) {

    public UpdateShowtimeRequest(
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            ShowtimeStatus status) {

        this(startsAt, endsAt, status, null, null);
    }

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null
                || endsAt == null
                || endsAt.isAfter(startsAt);
    }
}
