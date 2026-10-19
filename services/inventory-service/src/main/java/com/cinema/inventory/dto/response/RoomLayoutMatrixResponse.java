package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

public record RoomLayoutMatrixResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int rowCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int columnCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MatrixSeatResponse> seats) {

    public record MatrixSeatResponse(
            @JsonInclude(JsonInclude.Include.ALWAYS)
                    @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            nullable = true,
                            description =
                                    "Null while the draft cell is not bound to an inventory seat")
                    UUID seatId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int rowIndex,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int columnIndex,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int number,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SeatType seatType) {}
}
