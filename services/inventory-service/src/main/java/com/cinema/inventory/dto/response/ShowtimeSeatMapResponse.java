package com.cinema.inventory.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "ShowtimeSeatMapResponse",
        description =
                "Seat map from the showtime's pinned published layout. "
                        + "When matrix is non-null, use matrix dimensions and "
                        + "seat cell positions instead of legacy geometry.",
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
            "seats",
            "matrix"
        })
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ShowtimeSeatMapResponse(
        UUID showtimeId,
        UUID cinemaId,
        UUID roomId,
        UUID layoutId,
        long layoutVersion,
        OffsetDateTime serverTime,
        @Schema(
                        nullable = true,
                        description = "Legacy canvas width; null for matrix layouts.")
                BigDecimal canvasWidth,
        @Schema(
                        nullable = true,
                        description = "Legacy canvas height; null for matrix layouts.")
                BigDecimal canvasHeight,
        List<ShowtimeSeatMapElementResponse> elements,
        List<ShowtimeSeatMapSeatResponse> seats,
        @Schema(
                        nullable = true,
                        description =
                                "Matrix dimensions; null for legacy geometry layouts. "
                                        + "The screen is displayed above the matrix.")
                ShowtimeSeatMapMatrixResponse matrix) {

    @Schema(
            name = "ShowtimeSeatMapMatrixResponse",
            requiredProperties = {"rowCount", "columnCount"})
    public record ShowtimeSeatMapMatrixResponse(
            int rowCount,
            int columnCount) {}

    @Schema(
            name = "ShowtimeSeatMapSeatResponse",
            description =
                    "One inventory seat. COUPLE appears once and occupies two columns. "
                            + "Missing cells are empty spaces, not seats.",
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
                "selectable",
                "rowIndex",
                "columnIndex",
                "number",
                "columnSpan"
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
            @Schema(
                            nullable = true,
                            description = "Legacy geometry; null for matrix layouts.")
                    BigDecimal x,
            @Schema(
                            nullable = true,
                            description = "Legacy geometry; null for matrix layouts.")
                    BigDecimal y,
            @Schema(
                            nullable = true,
                            description = "Legacy geometry; null for matrix layouts.")
                    BigDecimal width,
            @Schema(
                            nullable = true,
                            description = "Legacy geometry; null for matrix layouts.")
                    BigDecimal height,
            @Schema(
                            nullable = true,
                            description = "Legacy geometry; null for matrix layouts.")
                    BigDecimal rotationDegrees,
            @Schema(
                            nullable = true,
                            description =
                                    "Price from ShowSeat; null when ShowSeat is missing.")
                    BigDecimal price,
            String currency,
            @Schema(
                            nullable = true,
                            description =
                                    "Stored ShowSeat status; null when ShowSeat is missing.")
                    ShowSeatStatus status,
            boolean selectable,
            @Schema(
                            nullable = true,
                            description =
                                    "Zero-based matrix row; null for legacy layouts.")
                    Integer rowIndex,
            @Schema(
                            nullable = true,
                            description =
                                    "Zero-based starting column; null for legacy layouts.")
                    Integer columnIndex,
            @Schema(
                            nullable = true,
                            description =
                                    "Consecutive seat number within the row; "
                                            + "null for legacy layouts.")
                    Integer number,
            @Schema(
                            nullable = true,
                            description =
                                    "Occupied columns: COUPLE=2, other types=1. "
                                            + "Null for legacy layouts.")
                    Integer columnSpan) {}

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
                            description =
                                    "Omitted when the layout element has no label.")
                    String label,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {}
}
