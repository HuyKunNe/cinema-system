package com.cinema.inventory.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.exception.InventoryErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RoomLayoutGeometry {

    @Column(name = "x", nullable = false, precision = 12, scale = 3)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 12, scale = 3)
    private BigDecimal y;

    @Column(name = "width", nullable = false, precision = 12, scale = 3)
    private BigDecimal width;

    @Column(name = "height", nullable = false, precision = 12, scale = 3)
    private BigDecimal height;

    @Column(
            name = "rotation_degrees",
            nullable = false,
            precision = 9,
            scale = 3)
    private BigDecimal rotationDegrees;

    protected RoomLayoutGeometry() {}

    public RoomLayoutGeometry(
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotationDegrees) {

        this.x = requireDecimal(x, 12);
        this.y = requireDecimal(y, 12);
        this.width = requirePositiveDimension(width);
        this.height = requirePositiveDimension(height);
        this.rotationDegrees = requireDecimal(rotationDegrees, 9);

        if (this.x.signum() < 0 || this.y.signum() < 0) {
            throw invalidGeometry();
        }
    }

    public BigDecimal getX() {
        return x;
    }

    public BigDecimal getY() {
        return y;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public BigDecimal getRotationDegrees() {
        return rotationDegrees;
    }

    static BigDecimal requirePositiveDimension(BigDecimal value) {
        BigDecimal normalized = requireDecimal(value, 12);

        if (normalized.signum() <= 0) {
            throw invalidGeometry();
        }

        return normalized;
    }

    private static BigDecimal requireDecimal(
            BigDecimal value,
            int precision) {

        if (value == null) {
            throw invalidGeometry();
        }

        BigDecimal normalized;

        try {
            normalized = value.setScale(3, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw invalidGeometry();
        }

        if (normalized.precision() > precision) {
            throw invalidGeometry();
        }

        return normalized;
    }

    private static ValidationException invalidGeometry() {
        return new ValidationException(
                InventoryErrorCode.ROOM_LAYOUT_GEOMETRY_INVALID);
    }
}
