package com.cinema.inventory.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.inventory.event.InventoryEventContract;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class BookingLifecyclePayloadTest {

    private static final OffsetDateTime CANCELLED_AT = OffsetDateTime.parse("2026-09-16T08:30:00Z");

    private static final OffsetDateTime EXPIRED_AT = OffsetDateTime.parse("2026-09-16T08:40:00Z");

    @Test
    void cancelledPayloadShouldPreserveCanonicalValues() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        bookingId,
                        userId,
                        showtimeId,
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        CANCELLED_AT);

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.reason())
                .isEqualTo(InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED);

        assertThat(payload.cancelledAt()).isEqualTo(CANCELLED_AT);
    }

    @Test
    void expiredPayloadShouldPreserveCanonicalValues() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        BookingExpiredPayload payload =
                new BookingExpiredPayload(bookingId, userId, showtimeId, EXPIRED_AT);

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.showtimeId()).isEqualTo(showtimeId);

        assertThat(payload.expiredAt()).isEqualTo(EXPIRED_AT);
    }
}
