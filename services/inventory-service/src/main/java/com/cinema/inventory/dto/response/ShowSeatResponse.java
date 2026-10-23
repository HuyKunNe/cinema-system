package com.cinema.inventory.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;

public record ShowSeatResponse(
        UUID id,
        UUID showtimeId,
        UUID seatId,
        String seatNumber,
        SeatType seatType,
        BigDecimal price,
        ShowSeatStatus status,
        UUID heldByBookingId,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}