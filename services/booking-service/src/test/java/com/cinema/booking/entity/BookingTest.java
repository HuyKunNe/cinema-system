package com.cinema.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.Test;

class BookingTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.of(
                    2026,
                    8,
                    20,
                    10,
                    0,
                    0,
                    0,
                    ZoneOffset.UTC);

    @Test
    void shouldCreatePendingBooking() {
        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();
        OffsetDateTime expiresAt = NOW.plusMinutes(10);

        Booking booking = new Booking(
                userId,
                showtimeId,
                "checkout-request-1",
                expiresAt,
                NOW);

        assertThat(booking.getUserId()).isEqualTo(userId);
        assertThat(booking.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(booking.getClientRequestId())
                .isEqualTo("checkout-request-1");
        assertThat(booking.getStatus())
                .isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getExpiresAt())
                .isEqualTo(expiresAt);
        assertThat(booking.getTotalAmount()).isNull();
        assertThat(booking.getCurrency()).isNull();
        assertThat(booking.getConfirmedAt()).isNull();
        assertThat(booking.getCancelledAt()).isNull();
        assertThat(booking.getRejectionReason()).isNull();
    }

    @Test
    void shouldTrimClientRequestId() {
        Booking booking = new Booking(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "  checkout-request-1  ",
                NOW.plusMinutes(10),
                NOW);

        assertThat(booking.getClientRequestId())
                .isEqualTo("checkout-request-1");
    }

    @Test
    void shouldRejectMissingUserId() {
        assertThrows(
                ValidationException.class,
                () -> new Booking(
                        null,
                        UUID.randomUUID(),
                        "checkout-request-1",
                        NOW.plusMinutes(10),
                        NOW));
    }

    @Test
    void shouldRejectMissingShowtimeId() {
        assertThrows(
                ValidationException.class,
                () -> new Booking(
                        UUID.randomUUID(),
                        null,
                        "checkout-request-1",
                        NOW.plusMinutes(10),
                        NOW));
    }

    @Test
    void shouldRejectBlankClientRequestId() {
        assertThrows(
                ValidationException.class,
                () -> new Booking(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        " ",
                        NOW.plusMinutes(10),
                        NOW));
    }

    @Test
    void shouldRejectExpirationAtCurrentTime() {
        assertThrows(
                ValidationException.class,
                () -> new Booking(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "checkout-request-1",
                        NOW,
                        NOW));
    }

    @Test
    void shouldRejectExpirationBeforeCurrentTime() {
        assertThrows(
                ValidationException.class,
                () -> new Booking(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "checkout-request-1",
                        NOW.minusSeconds(1),
                        NOW));
    }
}
