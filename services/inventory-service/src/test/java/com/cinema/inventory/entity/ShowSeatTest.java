package com.cinema.inventory.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;

class ShowSeatTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(
            2026,
            7,
            24,
            10,
            0,
            0,
            0,
            ZoneOffset.UTC);

    private UUID bookingId;
    private ShowSeat showSeat;

    @BeforeEach
    void setUp() {
        Cinema cinema = new Cinema(
                "Cinema One",
                "123 Main Street",
                "Ho Chi Minh City");

        Room room = new Room(
                cinema,
                "Room 1",
                RoomType.STANDARD);

        Seat seat = new Seat(
                room,
                "H7",
                "H",
                SeatType.STANDARD);

        Showtime showtime = new Showtime(
                UUID.randomUUID(),
                room,
                NOW.plusDays(1),
                NOW.plusDays(1).plusHours(2));

        bookingId = UUID.randomUUID();

        showSeat = new ShowSeat(
                showtime,
                seat,
                new BigDecimal("120000.00"));
    }

    @Test
    void shouldHoldAvailableSeat() {
        OffsetDateTime expiresAt = NOW.plusMinutes(10);

        showSeat.hold(bookingId, expiresAt, NOW);

        assertEquals(
                ShowSeatStatus.HELD,
                showSeat.getStatus());

        assertEquals(
                bookingId,
                showSeat.getHeldByBookingId());

        assertEquals(
                expiresAt,
                showSeat.getHoldExpiresAt());

        assertTrue(showSeat.isHeldBy(bookingId));
    }

    @Test
    void shouldBookSeatHeldBySameBooking() {
        showSeat.hold(
                bookingId,
                NOW.plusMinutes(10),
                NOW);

        showSeat.book(bookingId);

        assertEquals(
                ShowSeatStatus.BOOKED,
                showSeat.getStatus());

        assertNull(showSeat.getHeldByBookingId());
        assertNull(showSeat.getHoldExpiresAt());
    }

    @Test
    void shouldReleaseSeatHeldBySameBooking() {
        showSeat.hold(
                bookingId,
                NOW.plusMinutes(10),
                NOW);

        showSeat.release(bookingId);

        assertEquals(
                ShowSeatStatus.AVAILABLE,
                showSeat.getStatus());

        assertNull(showSeat.getHeldByBookingId());
        assertNull(showSeat.getHoldExpiresAt());
    }

    @Test
    void shouldRejectHoldWhenSeatIsAlreadyHeld() {
        showSeat.hold(
                bookingId,
                NOW.plusMinutes(10),
                NOW);

        assertThrows(
                IllegalStateException.class,
                () -> showSeat.hold(
                        UUID.randomUUID(),
                        NOW.plusMinutes(10),
                        NOW));
    }

    @Test
    void shouldRejectBookingFromDifferentBooking() {
        showSeat.hold(
                bookingId,
                NOW.plusMinutes(10),
                NOW);

        assertThrows(
                IllegalStateException.class,
                () -> showSeat.book(UUID.randomUUID()));
    }

    @Test
    void shouldReleaseExpiredHold() {
        showSeat.hold(
                bookingId,
                NOW.plusMinutes(10),
                NOW);

        assertFalse(
                showSeat.hasExpired(NOW.plusMinutes(9)));

        assertTrue(
                showSeat.hasExpired(NOW.plusMinutes(10)));

        showSeat.releaseExpiredHold(
                NOW.plusMinutes(10));

        assertEquals(
                ShowSeatStatus.AVAILABLE,
                showSeat.getStatus());
    }

    @Test
    void shouldRejectNegativePrice() {
        assertThrows(
                IllegalArgumentException.class,
                () -> showSeat.changePrice(
                        new BigDecimal("-1.00")));
    }
}
