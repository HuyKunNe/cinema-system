package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.RoomType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record RoomResponse(
        UUID id,
        UUID cinemaId,
        String name,
        RoomType roomType,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long version,
        List<ContentFormat> contentFormats) {

    public RoomResponse(
            UUID id,
            UUID cinemaId,
            String name,
            RoomType roomType,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        this(id, cinemaId, name, roomType, active, createdAt, updatedAt, null, List.of());
    }
}
