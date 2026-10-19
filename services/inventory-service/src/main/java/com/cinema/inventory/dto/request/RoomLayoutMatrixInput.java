package com.cinema.inventory.dto.request;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(
        description =
                "Sparse room matrix. Each seat is represented once at its starting cell. "
                        + "COUPLE occupies two adjacent columns; other types occupy one. "
                        + "Do not submit an additional seat for the second COUPLE cell.")
public record RoomLayoutMatrixInput(
        @NotNull
                @Min(1)
                @Max(100)
                @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                Integer rowCount,
        @NotNull
                @Min(1)
                @Max(100)
                @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                Integer columnCount,
        @NotNull
                @Size(max = 2500)
                List<@NotNull @Valid MatrixSeatInput> seats) {

    @Schema(name = "MatrixSeatInput")
    public record MatrixSeatInput(
            UUID seatId,
            @NotNull
                    @Min(0)
                    @Max(99)
                    @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                    Integer rowIndex,
            @NotNull
                    @Min(0)
                    @Max(99)
                    @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                    @Schema(
                            description =
                                    "Zero-based starting column. COUPLE also occupies "
                                            + "the next column in the same row.")
                    Integer columnIndex,
            @NotNull
                    @Min(1)
                    @Max(100)
                    @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                    @Schema(
                            description =
                                    "Consecutive seat number within the row, starting at 1. "
                                            + "COUPLE counts as one seat.")
                    Integer number,
            @Schema(
                            description =
                                    "Defaults to STANDARD. COUPLE occupies two columns; "
                                            + "all other types occupy one.")
                    SeatType seatType) {

        public MatrixSeatInput {
            if (seatType == null) {
                seatType = SeatType.STANDARD;
            }
        }
    }
}
