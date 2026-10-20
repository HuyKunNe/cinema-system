package com.cinema.inventory.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

public record CinemaResponse(
        UUID id,
        String name,
        String address,
        String city,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        example = "Asia/Ho_Chi_Minh",
                        description = "Null means the cinema time zone is not configured.")
                String timeZone) {

    public CinemaResponse(
            UUID id,
            String name,
            String address,
            String city,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        this(id, name, address, city, active, createdAt, updatedAt, null);
    }
}
