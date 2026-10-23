package com.cinema.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CinemaResponse(
        UUID id,
        String name,
        String address,
        String city,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String phone,
        String email,
        String openingHours,
        String directions,
        String timeZone) {

    public CinemaResponse(
            UUID id,
            String name,
            String address,
            String city,
            boolean active,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {

        this(id, name, address, city, active, createdAt, updatedAt, null, null, null, null, null);
    }
}
