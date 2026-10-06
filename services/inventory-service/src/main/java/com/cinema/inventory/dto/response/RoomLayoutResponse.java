package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.RoomLayoutStatus;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RoomLayoutResponse(
        UUID id,
        UUID roomId,
        long layoutVersion,
        RoomLayoutStatus status,
        BigDecimal canvasWidth,
        BigDecimal canvasHeight,
        @Schema(nullable = true, description = "Absent while the layout is a draft")
                OffsetDateTime publishedAt,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
