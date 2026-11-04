package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        requiredProperties = {
            "audioLanguage",
            "presentationType",
            "active"
        },
        description =
                "A complete movie-edit item. Definition fields are immutable for persisted IDs. "
                        + "Image format and projection technology default to 2D and STANDARD.")
public record SaveMovieScreeningVersionRequest(
        @Schema(
                        nullable = true,
                        description = "Existing version ID. Omit for a new version.")
                UUID id,
        @PositiveOrZero
                @Schema(
                        nullable = true,
                        minimum = "0",
                        description =
                                "Required for an existing version. "
                                        + "Omit for a new version.")
                Long expectedVersion,
        @Schema(defaultValue = "2D") ContentFormat contentFormat,
        @Schema(
                        defaultValue = "STANDARD",
                        description =
                                "Projection technology: STANDARD, IMAX, FOUR_DX or SCREEN_X.")
                ProjectionTechnology projectionTechnology,
        @NotBlank @Size(max = 100) String audioLanguage,
        @NotNull PresentationType presentationType,
        @Size(max = 100)
                @Schema(
                        nullable = true,
                        description =
                                "Required and nonblank for SUBTITLED. "
                                        + "Otherwise omit, send null or a blank string.")
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
