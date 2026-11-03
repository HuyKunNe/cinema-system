package com.cinema.movie.dto.response;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
@Schema(
        requiredProperties = {
            "id",
            "movieId",
            "contentFormat",
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
        String audioLanguage,
        PresentationType presentationType,
        @Schema(nullable = true) String subtitleLanguage,
        boolean active,
        @Schema(minimum = "0") Long version) {}
