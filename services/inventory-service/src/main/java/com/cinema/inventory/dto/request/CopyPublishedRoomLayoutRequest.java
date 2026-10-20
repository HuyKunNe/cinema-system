package com.cinema.inventory.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(
        description =
                "Creates a new matrix draft from a published layout. "
                        + "expectedVersion refers to the source layout's version field.")
public record CopyPublishedRoomLayoutRequest(
        @NotNull
                @PositiveOrZero
                @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description = "Expected source layout version, not layoutVersion.")
                Long expectedVersion) {}
