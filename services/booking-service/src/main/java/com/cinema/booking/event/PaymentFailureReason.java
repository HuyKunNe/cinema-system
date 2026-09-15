package com.cinema.booking.event;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;

import java.util.Arrays;
import java.util.Locale;

public enum PaymentFailureReason {
    PAYMENT_DECLINED,

    PAYMENT_TIMEOUT,

    PROVIDER_UNAVAILABLE,

    INVALID_PAYMENT_REQUEST,

    RESERVATION_EXPIRED,

    DUPLICATE_PAYMENT;

    public static PaymentFailureReason from(String value) {

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
