package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutElement;
import com.cinema.inventory.entity.RoomLayoutGeometry;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class RoomLayoutPublicationValidator {

    private static final double EPSILON = 0.000001;
    private static final BigDecimal FULL_ROTATION = BigDecimal.valueOf(360);

    public void validate(
            RoomLayout layout, List<RoomLayoutSeat> seats, List<RoomLayoutElement> elements) {

        if (seats.isEmpty()
                || elements.stream()
                        .noneMatch(element -> element.getKind() == RoomLayoutElementKind.SCREEN)) {

            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_INCOMPLETE);
        }

        Set<UUID> seatIds = new HashSet<>();
        List<Rectangle> seatRectangles = new ArrayList<>();

        for (RoomLayoutSeat position : seats) {
            Seat seat = position.getSeat();

            if (seat == null
                    || seat.getRoom() == null
                    || !layout.getRoom().getId().equals(seat.getRoom().getId())) {

                throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_SEAT_ROOM_MISMATCH);
            }

            if (!layout.getId().equals(position.getLayout().getId())
                    || position.getSeatNumberSnapshot() == null
                    || position.getSeatNumberSnapshot().isBlank()
                    || position.getRowLabelSnapshot() == null
                    || position.getRowLabelSnapshot().isBlank()
                    || position.getSeatTypeSnapshot() == null) {

                throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
            }

            if (!seatIds.add(seat.getId())) {
                throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DUPLICATE_SEAT);
            }

            seatRectangles.add(requireInsideCanvas(layout, position.getGeometry()));
        }

        List<Rectangle> elementRectangles = new ArrayList<>();

        for (RoomLayoutElement element : elements) {
            if (element.getKind() == null || !layout.getId().equals(element.getLayout().getId())) {

                throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
            }

            elementRectangles.add(requireInsideCanvas(layout, element.getGeometry()));
        }

        for (int index = 0; index < seatRectangles.size(); index++) {
            Rectangle seat = seatRectangles.get(index);

            for (int other = index + 1; other < seatRectangles.size(); other++) {

                rejectOverlap(seat, seatRectangles.get(other));
            }

            for (Rectangle element : elementRectangles) {
                rejectOverlap(seat, element);
            }
        }
    }

    private Rectangle requireInsideCanvas(RoomLayout layout, RoomLayoutGeometry geometry) {

        if (geometry == null) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_GEOMETRY_INVALID);
        }

        // Revalidate persisted values using the existing geometry rules.
        RoomLayoutGeometry validated =
                new RoomLayoutGeometry(
                        geometry.getX(),
                        geometry.getY(),
                        geometry.getWidth(),
                        geometry.getHeight(),
                        geometry.getRotationDegrees());

        double halfWidth = validated.getWidth().doubleValue() / 2;
        double halfHeight = validated.getHeight().doubleValue() / 2;

        double angle =
                StrictMath.toRadians(
                        validated.getRotationDegrees().remainder(FULL_ROTATION).doubleValue());

        Rectangle rectangle =
                new Rectangle(
                        validated.getX().doubleValue() + halfWidth,
                        validated.getY().doubleValue() + halfHeight,
                        halfWidth,
                        halfHeight,
                        StrictMath.cos(angle),
                        StrictMath.sin(angle));

        double extentX =
                halfWidth * StrictMath.abs(rectangle.cos())
                        + halfHeight * StrictMath.abs(rectangle.sin());

        double extentY =
                halfWidth * StrictMath.abs(rectangle.sin())
                        + halfHeight * StrictMath.abs(rectangle.cos());

        double canvasWidth = layout.getCanvasWidth().doubleValue();
        double canvasHeight = layout.getCanvasHeight().doubleValue();

        if (rectangle.centerX() - extentX < -EPSILON
                || rectangle.centerY() - extentY < -EPSILON
                || rectangle.centerX() + extentX > canvasWidth + EPSILON
                || rectangle.centerY() + extentY > canvasHeight + EPSILON) {

            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_OUTSIDE_CANVAS);
        }

        return rectangle;
    }

    private void rejectOverlap(Rectangle first, Rectangle second) {
        if (overlaps(first, second)) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_GEOMETRY_OVERLAP);
        }
    }

    private boolean overlaps(Rectangle first, Rectangle second) {
        return !separated(first, second, first.cos(), first.sin())
                && !separated(first, second, -first.sin(), first.cos())
                && !separated(first, second, second.cos(), second.sin())
                && !separated(first, second, -second.sin(), second.cos());
    }

    private boolean separated(Rectangle first, Rectangle second, double axisX, double axisY) {

        double distance =
                StrictMath.abs(
                        (second.centerX() - first.centerX()) * axisX
                                + (second.centerY() - first.centerY()) * axisY);

        double radius =
                projectedRadius(first, axisX, axisY) + projectedRadius(second, axisX, axisY);

        return distance >= radius - EPSILON;
    }

    private double projectedRadius(Rectangle rectangle, double axisX, double axisY) {

        return rectangle.halfWidth()
                        * StrictMath.abs(rectangle.cos() * axisX + rectangle.sin() * axisY)
                + rectangle.halfHeight()
                        * StrictMath.abs(-rectangle.sin() * axisX + rectangle.cos() * axisY);
    }

    private record Rectangle(
            double centerX,
            double centerY,
            double halfWidth,
            double halfHeight,
            double cos,
            double sin) {}
}
