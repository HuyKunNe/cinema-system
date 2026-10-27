package com.cinema.inventory.dto.request;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReplaceFareMatrixRequest(
        @NotNull @Min(0) Long expectedVersion,
        @NotNull @Size(max = 10)
                List<@NotNull @Valid ScopeInput> scopes,
        @NotNull @Size(max = 4)
                List<@NotNull SeatType> seatTypes,
        @NotNull
                List<@NotNull @Valid CellInput> cells) {

    @JsonAnySetter
    public void rejectUnknownProperty(String property, Object value) {
        throw new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
    }

    public record ScopeInput(
            @NotNull PricingExperienceType experienceType,
            @NotNull ContentFormat contentFormat) {}

    public record CellInput(
            @NotNull PricingExperienceType experienceType,
            @NotNull ContentFormat contentFormat,
            @NotNull UUID timeBandId,
            @NotNull UUID ticketGroupId,
            @NotNull SeatType seatType,
            @NotNull FareCellState state,
            @PositiveOrZero @Digits(integer = 10, fraction = 2)
                    BigDecimal amount) {

        @JsonAnySetter
        public void rejectUnknownProperty(String property, Object value) {
            throw new ValidationException(InventoryErrorCode.INVALID_FARE_MATRIX);
        }
    }
}
