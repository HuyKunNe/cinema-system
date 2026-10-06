package com.cinema.movie.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

public record MovieHeroConfigurationResponse(
        UUID movieId,
        boolean heroEnabled,
        int heroPriority,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(nullable = true)
        OffsetDateTime heroStartsAt,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        @Schema(nullable = true)
        OffsetDateTime heroEndsAt) {
}
