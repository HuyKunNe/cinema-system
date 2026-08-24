package com.cinema.booking.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class BookingExpiredPayloadTest {

    @Test
    void payloadShouldPreserveCanonicalValues() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        OffsetDateTime expiredAt = OffsetDateTime.parse("2026-08-24T10:10:00Z");

        BookingExpiredPayload payload =
                new BookingExpiredPayload(bookingId, userId, showtimeId, expiredAt);

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.expiredAt()).isEqualTo(expiredAt);
    }

    @Test
    void equalPayloadsShouldHaveValueEquality() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        OffsetDateTime expiredAt = OffsetDateTime.parse("2026-08-24T10:10:00Z");

        BookingExpiredPayload first =
                new BookingExpiredPayload(bookingId, userId, showtimeId, expiredAt);

        BookingExpiredPayload second =
                new BookingExpiredPayload(bookingId, userId, showtimeId, expiredAt);

        assertThat(second).isEqualTo(first);

        assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }
}
