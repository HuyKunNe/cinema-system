package com.cinema.inventory.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.ShowtimeStatus;

class ShowtimeTest {

    private static final OffsetDateTime START = OffsetDateTime.of(
            2026,
            7,
            25,
            10,
            0,
            0,
            0,
            ZoneOffset.UTC);

    @Test
    void shouldFollowShowtimeLifecycle() {
        Showtime showtime = createShowtime();

        assertEquals(
                ShowtimeStatus.SCHEDULED,
                showtime.getStatus());

        showtime.openForBooking();

        assertEquals(
                ShowtimeStatus.OPEN_FOR_BOOKING,
                showtime.getStatus());

        showtime.close();

        assertEquals(
                ShowtimeStatus.CLOSED,
                showtime.getStatus());

        showtime.complete();

        assertEquals(
                ShowtimeStatus.COMPLETED,
                showtime.getStatus());
    }

    @Test
    void shouldRejectInvalidTimeRange() {
        Room room = createRoom();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Showtime(
                        UUID.randomUUID(),
                        room,
                        START,
                        START));
    }

    @Test
    void shouldRejectOpeningTwice() {
        Showtime showtime = createShowtime();

        showtime.openForBooking();

        assertThrows(
                IllegalStateException.class,
                showtime::openForBooking);
    }

    private Showtime createShowtime() {
        return new Showtime(
                UUID.randomUUID(),
                createRoom(),
                START,
                START.plusHours(2));
    }

    private Room createRoom() {
        Cinema cinema = new Cinema(
                "Cinema One",
                "123 Main Street",
                "Ho Chi Minh City");

        return new Room(
                cinema,
                "Room 1",
                RoomType.STANDARD);
    }
}
