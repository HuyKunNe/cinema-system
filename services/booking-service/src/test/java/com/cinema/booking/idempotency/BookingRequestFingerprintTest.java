package com.cinema.booking.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

class BookingRequestFingerprintTest {

    private final BookingRequestFingerprint fingerprint = new BookingRequestFingerprint();

    @Test
    void sameNormalizedRequestShouldProduceSameFingerprint() {
        UUID showtimeId = UuidGenerator.next();

        String first = fingerprint.generate(showtimeId, List.of("H7", "H8"));

        String second = fingerprint.generate(showtimeId, List.of("H8", "H7"));

        assertThat(first).isEqualTo(second).hasSize(64);
    }

    @Test
    void differentSeatSetShouldProduceDifferentFingerprint() {
        UUID showtimeId = UuidGenerator.next();

        String first = fingerprint.generate(showtimeId, List.of("H7"));

        String second = fingerprint.generate(showtimeId, List.of("H8"));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void differentShowtimeShouldProduceDifferentFingerprint() {
        String first = fingerprint.generate(UuidGenerator.next(), List.of("H7"));

        String second = fingerprint.generate(UuidGenerator.next(), List.of("H7"));

        assertThat(first).isNotEqualTo(second);
    }
}
