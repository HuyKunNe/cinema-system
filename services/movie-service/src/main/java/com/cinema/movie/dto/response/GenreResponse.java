package com.cinema.movie.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GenreResponse(
        UUID id,
        String name,
        String description,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
