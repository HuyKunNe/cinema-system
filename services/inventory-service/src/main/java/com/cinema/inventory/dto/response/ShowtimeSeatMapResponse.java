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

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ShowtimeSeatMapSeatResponse(
            UUID seatId,
            @Schema(nullable = true) UUID showSeatId,
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

    public record ShowtimeSeatMapElementResponse(
            UUID id,
            RoomLayoutElementKind kind,
            @Schema(nullable = true) String label,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {}
}
