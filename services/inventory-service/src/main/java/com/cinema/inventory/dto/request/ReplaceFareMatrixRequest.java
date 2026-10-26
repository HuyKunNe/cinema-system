package com.cinema.inventory.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ReplaceFareMatrixRequest(
        @NotNull @Min(0) Long expectedVersion,
        @NotNull @Size(max = 10)
                List<@NotNull @Valid ScopeInput> scopes,
        @NotNull @Size(max = 4)
                List<@NotNull TicketCategory> ticketCategories,
        @NotNull @Size(max = 4)
                List<@NotNull SeatType> seatTypes,
        @NotNull @Size(max = 320)
                List<@NotNull @Valid CellInput> cells) {

    public record ScopeInput(
            @NotNull PricingExperienceType experienceType,
            @NotNull ContentFormat contentFormat) {}

    public record CellInput(
            @NotNull PricingExperienceType experienceType,
            @NotNull ContentFormat contentFormat,
            @NotNull PricingTimeBand timeBand,
            @NotNull TicketCategory ticketCategory,
            @NotNull SeatType seatType,
            @NotNull FareCellState state,
            @PositiveOrZero @Digits(integer = 10, fraction = 2)
                    BigDecimal amount) {}
}
