package com.cinema.movie.dto.request;

import com.cinema.movie.entity.AgeRating;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

public record UpdateMovieMetadataRequest(
        @JsonProperty(value = "backdropUrl", required = true)
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        maxLength = 500,
                        description = "Absolute HTTP/HTTPS URL. Null clears the artwork.")
                @Size(max = 500, message = "Backdrop URL must not exceed 500 characters")
                String backdropUrl,
        @JsonProperty(value = "ageRating", required = true)
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        nullable = true,
                        example = "T13",
                        description = "Confirmed classification. Null clears the classification.")
                AgeRating ageRating) {}
