package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(
        description =
                "Published matrix used to display room seats in administration. "
                        + "Seat labels, positions and types come from the layout. "
                        + "Active state comes from the inventory seat.",
        requiredProperties = {"layout", "matrix"})
public record RoomPublishedSeatMapResponse(
        RoomLayoutResponse layout, PublishedRoomMatrixResponse matrix) {

    @Schema(
            name = "PublishedRoomMatrixResponse",
            requiredProperties = {"rowCount", "columnCount", "seats"})
    public record PublishedRoomMatrixResponse(
            int rowCount, int columnCount, List<PublishedRoomSeatResponse> seats) {}

    @Schema(
            name = "PublishedRoomSeatResponse",
            description =
                    "One bound inventory seat at its starting matrix cell. "
                            + "A COUPLE seat appears once and occupies two columns.",
            requiredProperties = {
                "seatId",
                "rowIndex",
                "columnIndex",
                "number",
                "seatNumber",
                "rowLabel",
                "seatType",
                "capacity",
                "columnSpan",
                "active"
            })
    public record PublishedRoomSeatResponse(
            UUID seatId,
            @Schema(description = "Zero-based row index.") int rowIndex,
            @Schema(description = "Zero-based starting column index.") int columnIndex,
            @Schema(description = "Consecutive seat number within the row.") int number,
            @Schema(description = "Display label from the layout, such as A1.") String seatNumber,
            @Schema(description = "Row label: A through Z, then AA, AB and so on.") String rowLabel,
            SeatType seatType,
            @Schema(description = "People accommodated by this seat.") int capacity,
            @Schema(description = "Occupied columns: COUPLE=2, other types=1.") int columnSpan,
            @Schema(
                            description =
                                    "Current inventory active state. "
                                            + "This is not showtime booking availability.")
                    boolean active) {}
}
