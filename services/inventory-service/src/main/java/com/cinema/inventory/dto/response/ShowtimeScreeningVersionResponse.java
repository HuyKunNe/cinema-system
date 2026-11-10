package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.cinema.inventory.enums.ProjectionTechnology;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ShowtimeScreeningVersionResponse(
        UUID id,
        Long version,
        ContentFormat contentFormat,
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
