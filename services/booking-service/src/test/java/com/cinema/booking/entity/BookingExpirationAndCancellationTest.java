package com.cinema.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class BookingExpirationAndCancellationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    @Test
    void reservedBookingShouldBeCancelledBeforeExpiration() {

        Booking booking = reservedBooking();

        OffsetDateTime cancelledAt = NOW.plusMinutes(5);

        booking.cancel(cancelledAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isEqualTo(cancelledAt);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("90000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");
    }

    @Test
    void reservedBookingShouldExpireAtExpirationTime() {

        Booking booking = reservedBooking();

        booking.expire(EXPIRES_AT);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("90000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");
    }

    @Test
    void reservedBookingShouldExpireAfterExpirationTime() {

        Booking booking = reservedBooking();

        booking.expire(EXPIRES_AT.plusSeconds(1));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void cancellationAtExpirationTimeShouldBeRejected() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.cancel(EXPIRES_AT))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_RESERVATION_EXPIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getCancelledAt()).isNull();
    }

    @Test
    void cancellationAfterExpirationShouldBeRejected() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.cancel(EXPIRES_AT.plusSeconds(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void expirationBeforeExpirationTimeShouldBeRejected() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.expire(EXPIRES_AT.minusNanos(1)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_EXPIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void pendingBookingShouldNotBeCancelled() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.cancel(NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_RESERVED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void pendingBookingShouldNotExpire() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.expire(EXPIRES_AT)).isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void rejectedBookingShouldNotBeCancelled() {

        Booking booking = pendingBooking();

        booking.reject("SEAT_UNAVAILABLE");

        assertThatThrownBy(() -> booking.cancel(NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void rejectedBookingShouldNotExpire() {

        Booking booking = pendingBooking();

        booking.reject("SEAT_UNAVAILABLE");

        assertThatThrownBy(() -> booking.expire(EXPIRES_AT)).isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void cancelledBookingShouldNotBeCancelledAgain() {

        Booking booking = reservedBooking();

        booking.cancel(NOW.plusMinutes(1));

        assertThatThrownBy(() -> booking.cancel(NOW.plusMinutes(2)))
                .isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isEqualTo(NOW.plusMinutes(1));
    }

    @Test
    void expiredBookingShouldNotBeExpiredAgain() {

        Booking booking = reservedBooking();

        booking.expire(EXPIRES_AT);

        assertThatThrownBy(() -> booking.expire(EXPIRES_AT.plusSeconds(1)))
                .isInstanceOf(ConflictException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void cancellationShouldRequireCurrentTime() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.cancel(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.CURRENT_TIME_REQUIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);
    }

    @Test
    void expirationShouldRequireCurrentTime() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.expire(null)).isInstanceOf(ValidationException.class);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);
    }

    private Booking reservedBooking() {

        Booking booking = pendingBooking();

        booking.reserve(new BigDecimal("90000.00"), "VND");

        return booking;
    }

    private Booking pendingBooking() {

        return new Booking(
                UuidGenerator.next(),
                UuidGenerator.next(),
                "expiration-test-" + UuidGenerator.next(),
                "a".repeat(64),
                EXPIRES_AT,
                NOW);
    }
}
