package com.cinema.inventory.dto.request;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

@Schema(
        description =
                "Updates only the physical inventory seat's active state. "
                        + "Does not modify layout snapshots or existing show seats.")
public record UpdateSeatActiveRequest(
        @NotNull
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                UUID roomId,
        @NotNull
                @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                Boolean active) {}
