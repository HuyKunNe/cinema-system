package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.inventory.enums.SeatType;

public record SeatResponse(
        UUID id,
        UUID roomId,
        String seatNumber,
        String rowLabel,
        SeatType seatType,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}