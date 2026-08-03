package com.cinema.inventory.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

public record CreateShowtimeRequest(
        @NotNull UUID movieId,

        @NotNull UUID roomId,

        @NotNull @Future OffsetDateTime startsAt,

        @NotNull @Future OffsetDateTime endsAt,

        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal basePrice) {

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null
                || endsAt == null
                || endsAt.isAfter(startsAt);
    }
}
