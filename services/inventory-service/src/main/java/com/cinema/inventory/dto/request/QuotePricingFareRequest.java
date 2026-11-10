package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record QuotePricingFareRequest(
        @NotNull
                OffsetDateTime startsAt,
        @NotNull PricingExperienceType experienceType,
        @NotNull ContentFormat contentFormat,
                UUID ticketGroupId,
        @NotNull SeatType seatType) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(
                InventoryErrorCode.INVALID_PRICING_FARE_QUOTE);
    }
}
