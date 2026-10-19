package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(
        description =
                "Sparse room matrix. Each seat appears once at its starting cell. "
                        + "COUPLE occupies two adjacent columns without an additional "
                        + "seat entry for the second cell.")
public record RoomLayoutMatrixResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int rowCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int columnCount,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<MatrixSeatResponse> seats) {

    @Schema(name = "MatrixSeatResponse")
    public record MatrixSeatResponse(
            @JsonInclude(JsonInclude.Include.ALWAYS)
                    @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            nullable = true,
                            description =
                                    "Null while the draft cell is not bound to an inventory seat")
                    UUID seatId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int rowIndex,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description =
                                    "Zero-based starting column. COUPLE also occupies "
                                            + "the next column in the same row.")
                    int columnIndex,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description =
                                    "Consecutive seat number within the row. "
                                            + "COUPLE counts as one seat.")
                    int number,
            @Schema(
                            requiredMode = Schema.RequiredMode.REQUIRED,
                            description =
                                    "COUPLE occupies two columns; all other types occupy one.")
                    SeatType seatType) {}
}
