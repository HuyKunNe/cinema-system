package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.PresentationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ShowtimeScreeningVersionResponse(
        UUID id,
        Long version,
        ContentFormat contentFormat,
        String audioLanguage,
        PresentationType presentationType,
        String subtitleLanguage) {}
