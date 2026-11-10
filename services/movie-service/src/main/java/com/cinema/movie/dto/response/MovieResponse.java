package com.cinema.movie.dto.response;

import com.cinema.movie.entity.AgeRating;
import com.cinema.movie.entity.MovieStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

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
        @JsonInclude(JsonInclude.Include.ALWAYS) String backdropUrl,
        @JsonInclude(JsonInclude.Include.ALWAYS) AgeRating ageRating,
        @JsonInclude(JsonInclude.Include.NON_NULL)
                List<MovieScreeningVersionResponse> screeningVersions) {}
