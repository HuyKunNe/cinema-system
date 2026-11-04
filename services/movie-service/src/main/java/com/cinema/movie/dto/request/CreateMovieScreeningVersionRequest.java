package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(
        requiredProperties = {"audioLanguage", "presentationType"},
        description =
                "Screening-version definition. SUBTITLED requires a nonblank subtitleLanguage."
                    + " DUBBED and NO_SUBTITLES require an omitted, null or blank"
                    + " subtitleLanguage. Defaults to STANDARD projection in 2D.")
public record CreateMovieScreeningVersionRequest(
        @Schema(
                        description = "Image format. Wire values are 2D and 3D.",
                        defaultValue = "2D",
                        example = "2D")
                ContentFormat contentFormat,
        @Schema(
                        description =
                                "Projection technology: STANDARD, IMAX, FOUR_DX or SCREEN_X.",
                        defaultValue = "STANDARD",
                        example = "STANDARD")
                ProjectionTechnology projectionTechnology,
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
