package com.cinema.movie.dto.response;

import com.cinema.common.api.screening.ScreeningFormat.ScreeningFormatOption;
import com.cinema.movie.entity.MovieStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record MovieScreeningCapabilitiesResponse(
        UUID movieId,
        Long version,
        LocalDate releaseDate,
        MovieStatus status,
        List<ScreeningFormatOption> configuredFormats,
        List<ScreeningFormatOption> supportedFormatOptions) {}
