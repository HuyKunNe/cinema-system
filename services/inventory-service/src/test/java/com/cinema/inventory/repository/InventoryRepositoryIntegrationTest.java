package com.cinema.inventory.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cinema.inventory.InventoryServiceApplication;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@SpringBootTest(
        classes = InventoryServiceApplication.class)
@Transactional
class InventoryRepositoryIntegrationTest {

    private static final OffsetDateTime START = OffsetDateTime.of(
            2026,
            7,
            25,
            10,
            0,
            0,
            0,
            ZoneOffset.UTC);

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private ShowSeatRepository showSeatRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistCompleteInventoryGraph() {
        InventoryFixture fixture = createFixture();

        assertTrue(
                cinemaRepository.existsById(
                        fixture.cinema().getId()));

        assertTrue(
                roomRepository.existsByCinema_IdAndNameIgnoreCase(
                        fixture.cinema().getId(),
                        "room 1"));

        assertTrue(
                seatRepository.existsByRoom_IdAndSeatNumberIgnoreCase(
                        fixture.room().getId(),
                        "h7"));

        assertTrue(
                showSeatRepository.existsByShowtime_IdAndSeat_Id(
                        fixture.showtime().getId(),
                        fixture.seat().getId()));

        ShowSeat persistedShowSeat = showSeatRepository
                .findByShowtime_IdAndSeatNumberIgnoreCase(
                        fixture.showtime().getId(),
                        "h7")
                .orElseThrow();

        assertEquals(
                new BigDecimal("120000.00"),
                persistedShowSeat.getPrice());

        assertEquals(
                SeatType.STANDARD,
                persistedShowSeat.getSeatType());
    }

    @Test
    void shouldRejectDuplicateRoomNameWithinCinema() {
        Cinema cinema = cinemaRepository.save(
                new Cinema(
                        "Cinema One",
                        "123 Main Street",
                        "Ho Chi Minh City"));

        roomRepository.saveAndFlush(
                new Room(
                        cinema,
                        "Room 1",
                        RoomType.STANDARD));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> roomRepository.saveAndFlush(
                        new Room(
                                cinema,
                                "Room 1",
                                RoomType.IMAX)));
    }

    @Test
    void shouldRejectDuplicateSeatNumberWithinRoom() {
        Cinema cinema = cinemaRepository.save(
                new Cinema(
                        "Cinema One",
                        "123 Main Street",
                        "Ho Chi Minh City"));

        Room room = roomRepository.save(
                new Room(
                        cinema,
                        "Room 1",
                        RoomType.STANDARD));

        seatRepository.saveAndFlush(
                new Seat(
                        room,
                        "H7",
                        "H",
                        SeatType.STANDARD));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> seatRepository.saveAndFlush(
                        new Seat(
                                room,
                                "H7",
                                "H",
                                SeatType.VIP)));
    }

    @Test
    void shouldDetectOverlappingShowtime() {
        InventoryFixture fixture = createFixture();

        boolean overlaps = showtimeRepository
                .existsOverlappingShowtime(
                        fixture.room().getId(),
                        START.plusHours(1),
                        START.plusHours(3),
                        inactiveShowtimeStatuses());

        assertTrue(overlaps);
    }

    @Test
    void shouldAllowAdjacentShowtime() {
        InventoryFixture fixture = createFixture();

        boolean overlaps = showtimeRepository
                .existsOverlappingShowtime(
                        fixture.room().getId(),
                        START.plusHours(2),
                        START.plusHours(4),
                        inactiveShowtimeStatuses());

        assertFalse(overlaps);
    }

    @Test
    void shouldExcludeCurrentShowtimeWhenCheckingUpdate() {
        InventoryFixture fixture = createFixture();

        boolean overlaps = showtimeRepository
                .existsOverlappingShowtimeExcludingId(
                        fixture.room().getId(),
                        fixture.showtime().getId(),
                        START.plusMinutes(30),
                        START.plusHours(3),
                        inactiveShowtimeStatuses());

        assertFalse(overlaps);
    }

    @Test
    void shouldLockShowSeatsInStableSeatNumberOrder() {
        Cinema cinema = cinemaRepository.save(
                new Cinema(
                        "Cinema One",
                        "123 Main Street",
                        "Ho Chi Minh City"));

        Room room = roomRepository.save(
                new Room(
                        cinema,
                        "Room 1",
                        RoomType.STANDARD));

        Seat seatH8 = seatRepository.save(
                new Seat(
                        room,
                        "H8",
                        "H",
                        SeatType.STANDARD));

        Seat seatH7 = seatRepository.save(
                new Seat(
                        room,
                        "H7",
                        "H",
                        SeatType.STANDARD));

        Showtime showtime = showtimeRepository.save(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        START,
                        START.plusHours(2)));

        showSeatRepository.save(
                new ShowSeat(
                        showtime,
                        seatH8,
                        new BigDecimal("120000.00")));

        showSeatRepository.save(
                new ShowSeat(
                        showtime,
                        seatH7,
                        new BigDecimal("120000.00")));

        showSeatRepository.flush();
        entityManager.clear();

        List<ShowSeat> lockedSeats = showSeatRepository
                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                        showtime.getId(),
                        List.of("H8", "H7"));

        assertEquals(2, lockedSeats.size());
        assertEquals("H7", lockedSeats.get(0).getSeatNumber());
        assertEquals("H8", lockedSeats.get(1).getSeatNumber());
    }

    private InventoryFixture createFixture() {
        Cinema cinema = cinemaRepository.save(
                new Cinema(
                        "Cinema One",
                        "123 Main Street",
                        "Ho Chi Minh City"));

        Room room = roomRepository.save(
                new Room(
                        cinema,
                        "Room 1",
                        RoomType.STANDARD));

        Seat seat = seatRepository.save(
                new Seat(
                        room,
                        "H7",
                        "H",
                        SeatType.STANDARD));

        Showtime showtime = showtimeRepository.save(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        START,
                        START.plusHours(2)));

        ShowSeat showSeat = showSeatRepository.save(
                new ShowSeat(
                        showtime,
                        seat,
                        new BigDecimal("120000.00")));

        showSeatRepository.flush();

        return new InventoryFixture(
                cinema,
                room,
                seat,
                showtime,
                showSeat);
    }

    private EnumSet<ShowtimeStatus> inactiveShowtimeStatuses() {
        return EnumSet.of(
                ShowtimeStatus.CANCELLED,
                ShowtimeStatus.COMPLETED);
    }

    private record InventoryFixture(
            Cinema cinema,
            Room room,
            Seat seat,
            Showtime showtime,
            ShowSeat showSeat) {
    }
}
