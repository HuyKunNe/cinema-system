package com.cinema.booking.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.common.core.id.UuidGenerator;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class BookingCancelledPayloadTest {

    @Test
    void payloadShouldPreserveCanonicalValues() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        OffsetDateTime cancelledAt = OffsetDateTime.parse("2026-08-24T10:05:00Z");

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        BookingCancellationReason.USER_REQUESTED.name(),
                        cancelledAt);

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.reason()).isEqualTo("USER_REQUESTED");

        assertThat(payload.cancelledAt()).isEqualTo(cancelledAt);
    }

    @Test
    void equalPayloadsShouldHaveValueEquality() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        OffsetDateTime cancelledAt = OffsetDateTime.parse("2026-08-24T10:05:00Z");

        BookingCancelledPayload first =
                new BookingCancelledPayload(
                        bookingId, userId, showtimeId, "USER_REQUESTED", cancelledAt);

        BookingCancelledPayload second =
                new BookingCancelledPayload(
                        bookingId, userId, showtimeId, "USER_REQUESTED", cancelledAt);

        assertThat(second).isEqualTo(first);

        assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }
}
