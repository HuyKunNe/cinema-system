package com.cinema.movie.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.cinema.movie.entity.MovieStatus;

public record MovieResponse(
        UUID id,
        String title,
        String description,
        Integer durationMinutes,
        LocalDate releaseDate,
        String posterUrl,
        String trailerUrl,
        MovieStatus status,
        Set<GenreResponse> genres,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
