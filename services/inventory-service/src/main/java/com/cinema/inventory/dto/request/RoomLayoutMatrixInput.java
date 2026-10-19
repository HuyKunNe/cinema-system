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

public record RoomLayoutMatrixInput(
        @NotNull @Min(1) @Max(100) @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                Integer rowCount,
        @NotNull @Min(1) @Max(100) @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                Integer columnCount,
        @NotNull @Size(max = 2500) List<@NotNull @Valid MatrixSeatInput> seats) {

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
                    Integer columnIndex,
            @NotNull
                    @Min(1)
                    @Max(100)
                    @JsonDeserialize(using = StrictMatrixIntegerDeserializer.class)
                    Integer number,
            SeatType seatType) {

        public MatrixSeatInput {
            if (seatType == null) {
                seatType = SeatType.STANDARD;
            }
        }
    }
}
