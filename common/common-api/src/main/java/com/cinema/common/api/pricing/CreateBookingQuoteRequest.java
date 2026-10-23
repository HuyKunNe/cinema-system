package com.cinema.common.api.pricing;

import com.cinema.common.validation.annotation.UuidV5OrV7;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateBookingQuoteRequest(
        @NotNull @UuidV5OrV7 UUID showtimeId,
        @NotEmpty @Size(max = 100)
        List<@NotNull @Valid Item> items) {

    public record Item(
            @NotNull @UuidV5OrV7 UUID showSeatId,
            @NotBlank @Size(max = 64) String ticketCategoryCode,
            @UuidV7 UUID eligibilityApprovalId) {

        public Item(
                UUID showSeatId,
                String ticketCategoryCode) {

            this(showSeatId, ticketCategoryCode, null);
        }
    }
}
