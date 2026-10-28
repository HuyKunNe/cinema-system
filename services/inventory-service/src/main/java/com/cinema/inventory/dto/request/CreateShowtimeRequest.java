package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateShowtimeRequest(
        @NotNull UUID movieId,
        @NotNull UUID roomId,
        @NotNull OffsetDateTime startsAt,
        @NotNull OffsetDateTime endsAt,
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
                                        + "When supplied, generate seat fares using its default "
                                        + "ticket group. Omit or send null to schedule without "
                                        + "generating ShowSeats.")
                UUID pricingPolicyVersionId) {

    @JsonIgnore
    @AssertTrue(message = "endsAt must be after startsAt")
    public boolean isTimeRangeValid() {
        return startsAt == null || endsAt == null || endsAt.isAfter(startsAt);
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PRICING);
    }
}
