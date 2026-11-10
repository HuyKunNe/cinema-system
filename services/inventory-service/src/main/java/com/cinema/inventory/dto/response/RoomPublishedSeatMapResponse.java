package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;

import java.util.List;
import java.util.UUID;

public record RoomPublishedSeatMapResponse(
        RoomLayoutResponse layout, PublishedRoomMatrixResponse matrix) {

    public record PublishedRoomMatrixResponse(
            int rowCount, int columnCount, List<PublishedRoomSeatResponse> seats) {}

    public record PublishedRoomSeatResponse(
            UUID seatId,
            int rowIndex,
            int columnIndex,
            int number,
            String seatNumber,
            String rowLabel,
            SeatType seatType,
            int capacity,
            int columnSpan,
                    boolean active) {}
}
