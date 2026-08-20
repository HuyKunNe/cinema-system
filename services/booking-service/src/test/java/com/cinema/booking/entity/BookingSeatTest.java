package com.cinema.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

class BookingSeatTest {

    @Test
    void shouldCreatePendingSeatSnapshot() {
        UUID bookingId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        BookingSeat bookingSeat = new BookingSeat(bookingId, showtimeId, " h7 ");

        assertThat(bookingSeat.getBookingId()).isEqualTo(bookingId);
        assertThat(bookingSeat.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(bookingSeat.getSeatNumber()).isEqualTo("H7");

        assertThat(bookingSeat.getInventorySeatId()).isNull();
        assertThat(bookingSeat.getSeatType()).isNull();
        assertThat(bookingSeat.getPrice()).isNull();
        assertThat(bookingSeat.hasCompletedSnapshot()).isFalse();
    }

    @Test
    void shouldCompleteAuthoritativeSeatSnapshot() {
        BookingSeat bookingSeat = new BookingSeat(UUID.randomUUID(), UUID.randomUUID(), "H7");

        UUID inventorySeatId = UUID.randomUUID();

        bookingSeat.completeSnapshot(inventorySeatId, "standard", new BigDecimal("90000.00"));

        assertThat(bookingSeat.getInventorySeatId()).isEqualTo(inventorySeatId);
        assertThat(bookingSeat.getSeatType()).isEqualTo("STANDARD");
        assertThat(bookingSeat.getPrice()).isEqualByComparingTo("90000.00");
        assertThat(bookingSeat.hasCompletedSnapshot()).isTrue();
    }

    @Test
    void shouldRejectCompletingSnapshotTwice() {
        BookingSeat bookingSeat = new BookingSeat(UUID.randomUUID(), UUID.randomUUID(), "H7");

        bookingSeat.completeSnapshot(UUID.randomUUID(), "STANDARD", new BigDecimal("90000.00"));

        assertThrows(
                ConflictException.class,
                () ->
                        bookingSeat.completeSnapshot(
                                UUID.randomUUID(), "VIP", new BigDecimal("120000.00")));
    }

    @Test
    void shouldRejectNegativePrice() {
        BookingSeat bookingSeat = new BookingSeat(UUID.randomUUID(), UUID.randomUUID(), "H7");

        assertThrows(
                ValidationException.class,
                () ->
                        bookingSeat.completeSnapshot(
                                UUID.randomUUID(), "STANDARD", new BigDecimal("-1.00")));
    }

    @Test
    void shouldRejectBlankSeatNumber() {
        assertThrows(
                ValidationException.class,
                () -> new BookingSeat(UUID.randomUUID(), UUID.randomUUID(), " "));
    }
}
