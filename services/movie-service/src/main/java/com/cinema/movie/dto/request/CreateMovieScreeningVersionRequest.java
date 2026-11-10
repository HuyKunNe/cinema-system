package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMovieScreeningVersionRequest(
                ContentFormat contentFormat,
                ProjectionTechnology projectionTechnology,
        @NotBlank
                @Size(max = 100)
                String audioLanguage,
        @NotNull
                PresentationType presentationType,
        @Size(max = 100)
                String subtitleLanguage) {

    public CreateMovieScreeningVersionRequest {
        contentFormat = contentFormat == null ? ContentFormat.TWO_D : contentFormat;
        projectionTechnology =
                projectionTechnology == null
                        ? ProjectionTechnology.STANDARD
                        : projectionTechnology;
    }

    public CreateMovieScreeningVersionRequest(
            ContentFormat contentFormat,
            String audioLanguage,
            PresentationType presentationType,
            String subtitleLanguage) {

        this(
                contentFormat,
                ProjectionTechnology.STANDARD,
                audioLanguage,
                presentationType,
                subtitleLanguage);
    }
}
