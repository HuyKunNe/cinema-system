package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;

public record LayoutSeatSnapshot(Seat seat, String seatNumber, SeatType seatType) {

    public LayoutSeatSnapshot {
        if (seat == null
                || seat.getId() == null
                || seatNumber == null
                || seatNumber.isBlank()
                || seatNumber.length() > 20
                || seatType == null) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }
    }

    public static LayoutSeatSnapshot fromSeat(Seat seat) {
        if (seat == null) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        return new LayoutSeatSnapshot(seat, seat.getSeatNumber(), seat.getSeatType());
    }
}
