package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.SeatType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                """
                Geometry uses logical layout units.
                x and y identify the unrotated top-left corner.
                Rotation is around the rectangle center.
                Positive rotationDegrees rotate clockwise in screen coordinates.
                """)
public record RoomLayoutContentResponse(
        RoomLayoutResponse layout,
        List<RoomLayoutSeatPositionResponse> seats,
        List<RoomLayoutElementPositionResponse> elements) {

    public record RoomLayoutSeatPositionResponse(
            UUID id,
            UUID seatId,
            String seatNumberSnapshot,
            String rowLabelSnapshot,
            SeatType seatTypeSnapshot,
            RoomLayoutGeometryResponse geometry) {}

    public record RoomLayoutElementPositionResponse(
            UUID id,
            RoomLayoutElementKind kind,
            String label,
            RoomLayoutGeometryResponse geometry) {}

    public record RoomLayoutGeometryResponse(
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {}
}
