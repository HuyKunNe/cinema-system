package com.cinema.movie.dto.response;

import com.cinema.movie.entity.AgeRating;
import com.cinema.movie.entity.MovieStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        OffsetDateTime updatedAt,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Schema(nullable = true) String backdropUrl,
        @JsonInclude(JsonInclude.Include.ALWAYS) @Schema(nullable = true) AgeRating ageRating,
        @JsonInclude(JsonInclude.Include.NON_NULL)
                @Schema(nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                List<MovieScreeningVersionResponse> screeningVersions) {}
