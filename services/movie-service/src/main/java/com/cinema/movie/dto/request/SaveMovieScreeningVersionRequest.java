package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SaveMovieScreeningVersionRequest(
                UUID id,
        @PositiveOrZero
                Long expectedVersion,
        ContentFormat contentFormat,
                ProjectionTechnology projectionTechnology,
        @NotBlank @Size(max = 100) String audioLanguage,
        @NotNull PresentationType presentationType,
        @Size(max = 100)
                String subtitleLanguage,
        @NotNull Boolean active) {

    public SaveMovieScreeningVersionRequest {
        contentFormat = contentFormat == null ? ContentFormat.TWO_D : contentFormat;
        projectionTechnology =
                projectionTechnology == null
                        ? ProjectionTechnology.STANDARD
                        : projectionTechnology;
    }

    public SaveMovieScreeningVersionRequest(
            UUID id,
            Long expectedVersion,
            ContentFormat contentFormat,
            String audioLanguage,
            PresentationType presentationType,
            String subtitleLanguage,
            Boolean active) {

        this(
                id,
                expectedVersion,
                contentFormat,
                ProjectionTechnology.STANDARD,
                audioLanguage,
                presentationType,
                subtitleLanguage,
                active);
    }
}
