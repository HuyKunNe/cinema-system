package com.cinema.booking.service;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class BookingPickupTokenCodec {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "cinema-booking:v1:";

    public String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String parse(String payload) {
        if (payload == null || !payload.matches("^cinema-booking:v1:[A-Za-z0-9_-]{43}$")) {
            throw new ValidationException(BookingErrorCode.PICKUP_QR_INVALID);
        }
        String token = payload.substring(PREFIX.length());
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            if (bytes.length == 32
                    && Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(bytes)
                            .equals(token)) {
                return token;
            }
        } catch (IllegalArgumentException failure) {
            throw new ValidationException(BookingErrorCode.PICKUP_QR_INVALID);
        }
        throw new ValidationException(BookingErrorCode.PICKUP_QR_INVALID);
    }
}
