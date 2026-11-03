package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        requiredProperties = {"contentFormat", "audioLanguage", "presentationType"},
        description =
                "Screening-version definition. SUBTITLED requires a nonblank subtitleLanguage."
                    + " DUBBED and NO_SUBTITLES require an omitted, null or blank"
                    + " subtitleLanguage.")
public record CreateMovieScreeningVersionRequest(
        @NotNull @Schema(description = "Content format. Wire values are 2D and 3D.", example = "2D")
                ContentFormat contentFormat,
        @NotBlank
                @Size(max = 100)
                @Schema(
                        minLength = 1,
                        maxLength = 100,
                        description = "Audio language, trimmed before storage.",
                        example = "Tiếng Anh")
                String audioLanguage,
        @NotNull
                @Schema(
                        description = "Presentation type: SUBTITLED, DUBBED or NO_SUBTITLES.",
                        example = "SUBTITLED")
                PresentationType presentationType,
        @Size(max = 100)
                @Schema(
                        nullable = true,
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                        maxLength = 100,
                        description =
                                "Required and nonblank for SUBTITLED. "
                                        + "For DUBBED or NO_SUBTITLES, omit this property "
                                        + "or send null or a blank string.",
                        example = "Tiếng Việt")
                String subtitleLanguage) {}
