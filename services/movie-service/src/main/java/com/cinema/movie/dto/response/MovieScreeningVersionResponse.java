package com.cinema.movie.dto.response;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record MovieScreeningVersionResponse(
        UUID id,
        UUID movieId,
        ContentFormat contentFormat,
        String audioLanguage,
        PresentationType presentationType,
        String subtitleLanguage,
        boolean active,
        Long version) {}
