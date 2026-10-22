package com.cinema.booking.dto.request;

import com.cinema.common.validation.annotation.UuidV7;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBookingFromQuoteRequest(
        @NotBlank
                @Size(max = 100)
                String clientRequestId,
        @NotNull
                @UuidV7
                @Schema(description = "ID báo giá do Backend cấp", format = "uuid")
                UUID quoteId) {}
