package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PricingExperienceType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.TicketCategory;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record QuotePricingFareRequest(
        @NotNull
                @Schema(
                        description =
                                "Screening start including its UTC offset. "
                                        + "The cinema time zone determines the fare time band.")
                OffsetDateTime startsAt,
        @NotNull PricingExperienceType experienceType,
        @NotNull ContentFormat contentFormat,
        @NotNull TicketCategory ticketCategory,
        @NotNull SeatType seatType) {}
