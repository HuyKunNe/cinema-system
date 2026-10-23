package com.cinema.inventory.dto.request;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

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
        @NotNull
                @DecimalMin(value = "0.01")
                @Digits(integer = 10, fraction = 2)
                BigDecimal basePrice,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Published layout belonging to roomId. "
                                        + "Required when the room has a published matrix layout. "
                                        + "Otherwise omission retains legacy catalog generation. "
                                        + "The server does not automatically select a layout.")
                UUID roomLayoutId) {

    public CreateShowtimeRequest(
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            BigDecimal basePrice) {

        this(
                movieId,
                roomId,
                startsAt,
                endsAt,
                basePrice,
                null);
    }

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null
                || endsAt == null
                || endsAt.isAfter(startsAt);
    }
}
