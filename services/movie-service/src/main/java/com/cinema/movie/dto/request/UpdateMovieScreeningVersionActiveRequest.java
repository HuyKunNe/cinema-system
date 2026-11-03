package com.cinema.movie.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(requiredProperties = {"expectedVersion", "active"})
public record UpdateMovieScreeningVersionActiveRequest(
        @NotNull
                @PositiveOrZero
                @Schema(
                        minimum = "0",
                        description =
                                "Latest screening-version revision returned by the API. "
                                        + "A stale revision causes HTTP 409.",
                        example = "1")
                Long expectedVersion,
        @NotNull
                @Schema(
                        description = "Desired active state. Both true and false are valid.",
                        example = "true")
                Boolean active) {}
