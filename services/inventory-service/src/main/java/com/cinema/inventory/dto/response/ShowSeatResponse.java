package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShowSeatResponse(
        UUID id,
        UUID showtimeId,
        UUID seatId,
        String seatNumber,
        SeatType seatType,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        description = "Legacy price only. Null for policy-priced showtimes.")
                BigDecimal price,
        ShowSeatStatus status,
        UUID heldByBookingId,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
