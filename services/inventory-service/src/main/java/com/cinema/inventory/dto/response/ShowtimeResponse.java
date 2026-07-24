package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.cinema.inventory.enums.ShowtimeStatus;

public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        UUID roomId,
        String roomName,
        UUID cinemaId,
        String cinemaName,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        ShowtimeStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}