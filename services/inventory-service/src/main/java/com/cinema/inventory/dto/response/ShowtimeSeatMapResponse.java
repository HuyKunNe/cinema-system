package com.cinema.inventory.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
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
        List<ShowtimeSeatMapSeatResponse> seats,
                ShowtimeSeatMapMatrixResponse matrix) {

    public record ShowtimeSeatMapMatrixResponse(
            int rowCount,
            int columnCount) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ShowtimeSeatMapSeatResponse(
            UUID seatId,
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
                    BigDecimal price,
            String currency,
                    ShowSeatStatus status,
            boolean selectable,
                    Integer rowIndex,
                    Integer columnIndex,
                    Integer number,
                    Integer columnSpan) {}

    public record ShowtimeSeatMapElementResponse(
            UUID id,
            RoomLayoutElementKind kind,
                    String label,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {}
}
