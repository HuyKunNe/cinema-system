package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ShowtimeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
                UUID roomLayoutId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        description =
                                "Screening version snapshot pinned at creation. "
                                        + "Null for legacy showtimes.")
                ShowtimeScreeningVersionResponse screeningVersion,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        description = "Pinned scheduling details. Null for legacy showtimes.")
                ShowtimeSchedulingResponse scheduling,
        Long version) {

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
                null,
                null);
    }

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
            OffsetDateTime updatedAt,
            UUID roomLayoutId) {

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
                roomLayoutId,
                null);
    }

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
            OffsetDateTime updatedAt,
            UUID roomLayoutId,
            ShowtimeScreeningVersionResponse screeningVersion) {

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
                roomLayoutId,
                screeningVersion,
                null,
                null);
    }
}
