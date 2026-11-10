package com.cinema.movie.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

public record MovieHeroConfigurationResponse(
        UUID movieId,
        boolean heroEnabled,
        int heroPriority,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        OffsetDateTime heroStartsAt,

        @JsonInclude(JsonInclude.Include.ALWAYS)
        OffsetDateTime heroEndsAt) {
}
