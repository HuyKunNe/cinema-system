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

class BookingReservationResultTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    @Test
    void pendingBookingShouldBecomeReserved() {

        Booking booking = pendingBooking();

        booking.reserve(new BigDecimal("210000.00"), "vnd");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("210000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");

        assertThat(booking.getRejectionReason()).isNull();
    }

    @Test
    void pendingBookingShouldBecomeRejected() {

        Booking booking = pendingBooking();

        booking.reject("SEAT_NOT_AVAILABLE");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(booking.getRejectionReason()).isEqualTo("SEAT_NOT_AVAILABLE");

        assertThat(booking.getTotalAmount()).isNull();

        assertThat(booking.getCurrency()).isNull();
    }

    @Test
    void reservedBookingShouldNotBeReservedAgain() {

        Booking booking = pendingBooking();

        booking.reserve(new BigDecimal("90000.00"), "VND");

        assertThatThrownBy(() -> booking.reserve(new BigDecimal("90000.00"), "VND"))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.BOOKING_NOT_PENDING));
    }

    @Test
    void rejectedBookingShouldNotBecomeReserved() {

        Booking booking = pendingBooking();

        booking.reject("SEAT_NOT_AVAILABLE");

        assertThatThrownBy(() -> booking.reserve(new BigDecimal("90000.00"), "VND"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reservationShouldRequireTotalAmount() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.reserve(null, "VND"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reservationShouldRejectNegativeTotalAmount() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.reserve(new BigDecimal("-1.00"), "VND"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reservationShouldRequireIsoCurrency() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.reserve(new BigDecimal("90000.00"), "VN"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectionShouldRequireReason() {

        Booking booking = pendingBooking();

        assertThatThrownBy(() -> booking.reject(" ")).isInstanceOf(ValidationException.class);
    }

    private Booking pendingBooking() {

        return new Booking(
                UuidGenerator.next(),
                UuidGenerator.next(),
                "request-001",
                "a".repeat(64),
                NOW.plusMinutes(10),
                NOW);
    }
}
