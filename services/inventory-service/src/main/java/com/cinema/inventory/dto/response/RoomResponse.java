package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.inventory.enums.RoomType;

public record RoomResponse(
        UUID id,
        UUID cinemaId,
        String name,
        RoomType roomType,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}