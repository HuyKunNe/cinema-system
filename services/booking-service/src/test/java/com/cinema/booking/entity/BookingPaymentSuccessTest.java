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

class BookingPaymentSuccessTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-14T09:00:00Z");

    @Test
    void reservedBookingShouldBecomeConfirmedBeforeExpiration() {

        Booking booking = reservedBooking();

        OffsetDateTime confirmedAt = NOW.plusMinutes(1);

        booking.confirm(confirmedAt);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isEqualTo(confirmedAt);

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("180000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");
    }

    @Test
    void confirmationShouldRequireCurrentTime() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.confirm(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.CURRENT_TIME_REQUIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getConfirmedAt()).isNull();
    }

    @Test
    void pendingBookingShouldNotBecomeConfirmed() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.confirm(NOW.plusMinutes(1)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_RESERVED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(booking.getConfirmedAt()).isNull();
    }

    @Test
    void bookingShouldNotBeConfirmedAtExpirationBoundary() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.confirm(booking.getExpiresAt()))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_RESERVATION_EXPIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getConfirmedAt()).isNull();
    }

    @Test
    void bookingShouldNotBeConfirmedAfterExpiration() {

        Booking booking = reservedBooking();

        assertThatThrownBy(() -> booking.confirm(booking.getExpiresAt().plusNanos(1)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_RESERVATION_EXPIRED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getConfirmedAt()).isNull();
    }

    @Test
    void confirmedBookingShouldNotBeConfirmedAgain() {

        Booking booking = reservedBooking();

        booking.confirm(NOW.plusMinutes(1));

        OffsetDateTime originalConfirmedAt = booking.getConfirmedAt();

        assertThatThrownBy(() -> booking.confirm(NOW.plusMinutes(2)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_RESERVED));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isEqualTo(originalConfirmedAt);
    }

    private static Booking reservedBooking() {

        Booking booking = pendingBooking();

        booking.reserve(new BigDecimal("180000.00"), "VND");

        return booking;
    }

    private static Booking pendingBooking() {

        return new Booking(
                UuidGenerator.next(),
                UuidGenerator.next(),
                "payment-success-request",
                "a".repeat(64),
                NOW.plusMinutes(10),
                NOW);
    }
}
