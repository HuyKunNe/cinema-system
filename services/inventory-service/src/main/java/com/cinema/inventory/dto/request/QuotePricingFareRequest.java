package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QuotePricingFareRequest(
        @NotNull
                @Schema(
                        description =
                                "Screening start including its UTC offset. "
                                        + "The cinema time zone determines the fare time band.")
                OffsetDateTime startsAt,
        @NotNull PricingExperienceType experienceType,
        @NotNull ContentFormat contentFormat,
        @Schema(
                description =
                        "Ticket group owned by this pricing version. "
                                + "Omit to use the version's default group.")
                UUID ticketGroupId,
        @NotNull SeatType seatType) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(
                InventoryErrorCode.INVALID_PRICING_FARE_QUOTE);
    }
}
