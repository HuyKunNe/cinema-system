package com.cinema.inventory.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateRoomLayoutRequest(
        @NotNull UUID roomId,

        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 9, fraction = 3)
        BigDecimal canvasWidth,

        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 9, fraction = 3)
        BigDecimal canvasHeight,

        @Valid RoomLayoutMatrixInput matrix) {

    public CreateRoomLayoutRequest {
        if (canvasWidth == null && canvasHeight == null && matrix == null) {
            matrix = new RoomLayoutMatrixInput(6, 6, List.of());
        }
    }

    public CreateRoomLayoutRequest(
            UUID roomId,
            BigDecimal canvasWidth,
            BigDecimal canvasHeight) {

        this(roomId, canvasWidth, canvasHeight, null);
    }

    @JsonIgnore
    @AssertTrue(message = """
            Supply both legacy canvas dimensions or an empty matrix, not both
            """)
    public boolean isRepresentationValid() {
        if (matrix != null) {
            return canvasWidth == null
                    && canvasHeight == null
                    && matrix.seats() != null
                    && matrix.seats().isEmpty();
        }

        return canvasWidth != null && canvasHeight != null;
    }
}
