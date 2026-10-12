package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        description =
                """
                Geometry uses logical layout units.
                x and y identify the unrotated top-left corner.
                Rotation is around the rectangle center.
                Positive rotationDegrees rotate clockwise in screen coordinates.
                """)
public record RoomLayoutContentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RoomLayoutResponse layout,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<RoomLayoutSeatPositionResponse> seats,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                List<RoomLayoutElementPositionResponse> elements) {

    public record RoomLayoutSeatPositionResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID seatId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String seatNumberSnapshot,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String rowLabelSnapshot,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SeatType seatTypeSnapshot,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                    RoomLayoutGeometryResponse geometry) {}

    public record RoomLayoutElementPositionResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RoomLayoutElementKind kind,
            @Schema(nullable = true) String label,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                    RoomLayoutGeometryResponse geometry) {}

    public record RoomLayoutGeometryResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal x,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal y,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal width,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal height,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) BigDecimal rotationDegrees) {}
}
