package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.enums.ProjectionTechnology;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ShowtimeScreeningVersionResponse(
        UUID id,
        Long version,
        @Schema(description = "Image format. Wire values are 2D and 3D.")
        ContentFormat contentFormat,
        @Schema(description = "Projection technology pinned when the showtime was created.")
        ProjectionTechnology projectionTechnology,
        String audioLanguage,
        PresentationType presentationType,
        String subtitleLanguage) {

    public ShowtimeScreeningVersionResponse {
        projectionTechnology =
                projectionTechnology == null
                        ? ProjectionTechnology.STANDARD
                        : projectionTechnology;
    }
}
