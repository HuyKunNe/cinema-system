package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

public record RoomLayoutMatrixResponse(
        int rowCount,
        int columnCount,
        List<MatrixSeatResponse> seats) {

    public record MatrixSeatResponse(
            @JsonInclude(JsonInclude.Include.ALWAYS)
                    UUID seatId,
            int rowIndex,
                    int columnIndex,
                    int number,
                    SeatType seatType) {}
}
