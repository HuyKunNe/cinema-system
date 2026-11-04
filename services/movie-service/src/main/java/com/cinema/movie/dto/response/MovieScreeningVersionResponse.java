package com.cinema.movie.dto.response;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        requiredProperties = {
            "id",
            "movieId",
            "contentFormat",
            "projectionTechnology",
            "audioLanguage",
            "presentationType",
            "subtitleLanguage",
            "active",
            "version"
        })
public record MovieScreeningVersionResponse(
        UUID id,
        UUID movieId,
        @Schema(description = "Content format. Wire values are 2D and 3D.")
                ContentFormat contentFormat,
        @Schema(description = "Projection technology independent from the 2D/3D image format.")
                ProjectionTechnology projectionTechnology,
        String audioLanguage,
        PresentationType presentationType,
        @Schema(nullable = true) String subtitleLanguage,
        boolean active,
        @Schema(minimum = "0") Long version) {}
