package com.cinema.booking.event;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;

import java.util.Arrays;
import java.util.Locale;

public enum SeatReservationRejectionReason {
    SEAT_NOT_FOUND,

    SEAT_UNAVAILABLE,

    DUPLICATE_SEAT,

    INVALID_REQUEST,

    RESERVATION_EXPIRED,

    INVENTORY_CONFLICT;

    public static SeatReservationRejectionReason from(String value) {

        if (value == null || value.isBlank()) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(reason -> reason.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID));
    }
}
