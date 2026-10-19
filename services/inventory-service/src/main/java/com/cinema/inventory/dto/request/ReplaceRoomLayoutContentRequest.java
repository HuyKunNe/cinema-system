package com.cinema.inventory.dto.request;

import com.cinema.inventory.entity.RoomLayoutGeometry;
import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(
        description =
                """
                Replaces draft content.
                Supply matrix for matrix layouts.
                Supply seats and elements for legacy geometry layouts.
                The two representations are mutually exclusive.
                expectedVersion refers to layout.version.
                """)
public record ReplaceRoomLayoutContentRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        List<@NotNull @Valid RoomLayoutSeatInput> seats,
        List<@NotNull @Valid RoomLayoutElementInput> elements,
        @Valid RoomLayoutMatrixInput matrix) {

    public ReplaceRoomLayoutContentRequest(
            Long expectedVersion,
            List<RoomLayoutSeatInput> seats,
            List<RoomLayoutElementInput> elements) {

        this(expectedVersion, seats, elements, null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(
            message =
                    """
                    Supply matrix or both legacy seats and elements, not both
                    """)
    public boolean isRepresentationValid() {
        if (matrix != null) {
            return seats == null && elements == null;
        }

        return seats != null && elements != null;
    }

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
