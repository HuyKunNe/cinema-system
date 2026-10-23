package com.cinema.movie.dto.request;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.PresentationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMovieScreeningVersionRequest(
        @NotNull ContentFormat contentFormat,
        @NotBlank @Size(max = 100) String audioLanguage,
        @NotNull PresentationType presentationType,
        @Size(max = 100) String subtitleLanguage) {}
