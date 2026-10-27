package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateShowtimeRequest(
        @NotNull UUID movieId,
        @NotNull UUID roomId,
        @NotNull @Future OffsetDateTime startsAt,
        @NotNull @Future OffsetDateTime endsAt,
        @DecimalMin(value = "0.01")
                @Digits(integer = 10, fraction = 2)
                @Schema(
                        deprecated = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Legacy provisional price. Required only when no pricing "
                                        + "version is selected. Must be omitted when "
                                        + "pricingPolicyVersionId is supplied.")
                BigDecimal basePrice,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Published layout belonging to roomId. "
                                        + "Required when the room has a published matrix layout.")
                UUID roomLayoutId,
        @NotNull
                @Schema(
                        description =
                                "Active screening version belonging to movieId. "
                                        + "Its content format must be supported by the room.")
                UUID movieScreeningVersionId,
        @NotNull
                @Min(0)
                @Max(120)
                @Schema(description = "Room turnaround time after the screening, in minutes.")
                Integer breakMinutes,
        @Schema(
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        nullable = true,
                        description =
                                "Exact published pricing version applicable to the room's cinema. "
                                        + "Seat prices use its configured default ticket group.")
                UUID pricingPolicyVersionId) {

    public CreateShowtimeRequest(
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            BigDecimal basePrice,
            UUID roomLayoutId,
            UUID movieScreeningVersionId,
            Integer breakMinutes) {

        this(
                movieId,
                roomId,
                startsAt,
                endsAt,
                basePrice,
                roomLayoutId,
                movieScreeningVersionId,
                breakMinutes,
                null);
    }

    public CreateShowtimeRequest(
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            BigDecimal basePrice,
            UUID roomLayoutId) {

        this(movieId, roomId, startsAt, endsAt, basePrice, roomLayoutId, null);
    }

    public CreateShowtimeRequest(
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            BigDecimal basePrice) {

        this(movieId, roomId, startsAt, endsAt, basePrice, null, null);
    }

    public CreateShowtimeRequest(
            UUID movieId,
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            BigDecimal basePrice,
            UUID roomLayoutId,
            UUID movieScreeningVersionId) {

        this(
                movieId,
                roomId,
                startsAt,
                endsAt,
                basePrice,
                roomLayoutId,
                movieScreeningVersionId,
                null);
    }

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }

    @JsonIgnore
    @AssertTrue(message = "basePrice must be omitted when pricingPolicyVersionId is supplied")
    public boolean isPricingSourceValid() {
        return basePrice == null || pricingPolicyVersionId == null;
    }
}
