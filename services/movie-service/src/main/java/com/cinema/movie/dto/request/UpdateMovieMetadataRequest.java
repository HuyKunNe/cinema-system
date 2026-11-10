package com.cinema.movie.dto.request;

import com.cinema.movie.entity.AgeRating;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;

public record UpdateMovieMetadataRequest(
        @JsonProperty(value = "backdropUrl", required = true)
                @Size(max = 500, message = "Backdrop URL must not exceed 500 characters")
                String backdropUrl,
        @JsonProperty(value = "ageRating", required = true)
                AgeRating ageRating) {}
