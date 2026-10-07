package com.cinema.inventory.dto.request;

import com.cinema.inventory.entity.RoomLayoutGeometry;
import com.cinema.inventory.enums.RoomLayoutElementKind;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description =
                """
                Geometry uses logical layout units.
                x and y identify the unrotated top-left corner.
                Rotation is around the rectangle center.
                Positive rotationDegrees rotate clockwise in screen coordinates.
                """)
public record ReplaceRoomLayoutContentRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotNull List<@NotNull @Valid RoomLayoutSeatInput> seats,
        @NotNull List<@NotNull @Valid RoomLayoutElementInput> elements) {

    public record RoomLayoutSeatInput(
            @NotNull UUID seatId, @NotNull @Valid RoomLayoutGeometryInput geometry) {}

    public record RoomLayoutElementInput(
            @NotNull RoomLayoutElementKind kind,
            @Size(max = 150) String label,
            @NotNull @Valid RoomLayoutGeometryInput geometry) {}

    public record RoomLayoutGeometryInput(
            @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 3) BigDecimal x,
            @NotNull @DecimalMin("0") @Digits(integer = 9, fraction = 3) BigDecimal y,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
                    BigDecimal width,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
                    BigDecimal height,
            @NotNull @Digits(integer = 6, fraction = 3) BigDecimal rotationDegrees) {

        public RoomLayoutGeometry toGeometry() {
            return new RoomLayoutGeometry(x, y, width, height, rotationDegrees);
        }
    }
}
