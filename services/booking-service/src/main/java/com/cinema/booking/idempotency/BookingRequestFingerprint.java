package com.cinema.booking.idempotency;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.InternalServerException;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Component
public class BookingRequestFingerprint {

    private static final String ALGORITHM = "SHA-256";

    private static final String FIELD_SEPARATOR = "|";

    private static final String SEAT_SEPARATOR = ",";

    public String generate(UUID showtimeId, List<String> normalizedSeatNumbers) {

        String canonicalRequest =
                showtimeId
                        + FIELD_SEPARATOR
                        + String.join(
                                SEAT_SEPARATOR, normalizedSeatNumbers.stream().sorted().toList());

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ALGORITHM);

            byte[] digest = messageDigest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(digest);

        } catch (NoSuchAlgorithmException exception) {
            throw new InternalServerException(
                    BookingErrorCode.REQUEST_FINGERPRINT_GENERATION_FAILED, exception);
        }
    }
}
