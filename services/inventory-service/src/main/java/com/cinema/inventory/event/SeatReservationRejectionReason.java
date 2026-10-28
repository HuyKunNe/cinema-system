package com.cinema.inventory.event;

public enum SeatReservationRejectionReason {
    SEAT_NOT_FOUND,

    SEAT_UNAVAILABLE,

    DUPLICATE_SEAT,

    INVALID_REQUEST,

    RESERVATION_EXPIRED,

    PRICE_CHANGED,

    INVENTORY_CONFLICT
}
