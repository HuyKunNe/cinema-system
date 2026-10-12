package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.RoomLayoutStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomLayoutResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID roomId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "Layout revision number within the room; not the optimistic lock"
                                    + " version")
                long layoutVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RoomLayoutStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal canvasWidth,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal canvasHeight,
        @Schema(nullable = true, description = "Absent while the layout is a draft")
                OffsetDateTime publishedAt,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "Optimistic lock version used as expectedVersion when saving or"
                                    + " publishing")
                Long version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime createdAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OffsetDateTime updatedAt) {}
