package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ShowtimeStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
                UUID roomLayoutId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                ShowtimeScreeningVersionResponse screeningVersion,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                ShowtimeSchedulingResponse scheduling,
        Long version,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                UUID pricingPolicyId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                UUID pricingPolicyVersionId) {

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
            ShowtimeScreeningVersionResponse screeningVersion,
            ShowtimeSchedulingResponse scheduling,
            Long version) {

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
                scheduling,
                version,
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
