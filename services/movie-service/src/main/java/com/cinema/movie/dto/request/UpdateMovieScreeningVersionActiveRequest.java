package com.cinema.movie.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateMovieScreeningVersionActiveRequest(
        @NotNull @PositiveOrZero Long expectedVersion, @NotNull Boolean active) {}
