package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(
        requiredProperties = {
            "contentFormat",
            "audioLanguage",
            "presentationType",
            "active"
        })
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
        @NotNull ContentFormat contentFormat,
        @NotBlank @Size(max = 100) String audioLanguage,
        @NotNull PresentationType presentationType,
        @Size(max = 100)
                @Schema(
                        nullable = true,
                        description =
                                "Required and nonblank for SUBTITLED. "
                                        + "Otherwise omit, send null or a blank string.")
                String subtitleLanguage,
        @NotNull Boolean active) {}
