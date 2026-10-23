package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ShowtimeStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

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
        OffsetDateTime updatedAt,
        @Schema(
                        nullable = true,
                        description = "Published layout pinned when the showtime was created.")
                UUID roomLayoutId) {

    public ShowtimeResponse(
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

        this(
                id,
                movieId,
                roomId,
                roomName,
                cinemaId,
                cinemaName,
                startsAt,
                endsAt,
                status,
                createdAt,
                updatedAt,
                null);
    }
}
