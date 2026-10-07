package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "ShowtimeSeatMapResponse",
        requiredProperties = {
            "showtimeId",
            "cinemaId",
            "roomId",
            "layoutId",
            "layoutVersion",
            "serverTime",
            "canvasWidth",
            "canvasHeight",
            "elements",
            "seats"
        })
public record ShowtimeSeatMapResponse(
        UUID showtimeId,
        UUID cinemaId,
        UUID roomId,
        UUID layoutId,
        long layoutVersion,
        OffsetDateTime serverTime,
        BigDecimal canvasWidth,
        BigDecimal canvasHeight,
        List<ShowtimeSeatMapElementResponse> elements,
        List<ShowtimeSeatMapSeatResponse> seats) {

    @Schema(
            name = "ShowtimeSeatMapSeatResponse",
            requiredProperties = {
                "seatId",
                "showSeatId",
                "seatNumber",
                "rowLabel",
                "seatType",
                "capacity",
                "x",
                "y",
                "width",
                "height",
                "rotationDegrees",
                "price",
                "currency",
                "status",
                "selectable"
            })
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ShowtimeSeatMapSeatResponse(
            UUID seatId,
            @Schema(nullable = true)
                    UUID showSeatId,
            String seatNumber,
            String rowLabel,
            SeatType seatType,
            int capacity,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees,
            @Schema(
                            nullable = true,
                            description = "Price from ShowSeat; null when ShowSeat is missing.")
                    BigDecimal price,
            String currency,
            @Schema(
                            nullable = true,
                            description = "Stored ShowSeat status; null when ShowSeat is missing.")
                    ShowSeatStatus status,
            boolean selectable) {}

    @Schema(
            name = "ShowtimeSeatMapElementResponse",
            requiredProperties = {
                "id",
                "kind",
                "x",
                "y",
                "width",
                "height",
                "rotationDegrees"
            })
    public record ShowtimeSeatMapElementResponse(
            UUID id,
            RoomLayoutElementKind kind,
            @Schema(
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                            description = "Omitted when the layout element has no label.")
                    String label,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {}
}
