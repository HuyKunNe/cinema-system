package com.cinema.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class SeatRepositoryTest {

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EntityManager entityManager;

    private Room room;

    @BeforeEach
    void setUp() {
        Cinema cinema = cinemaRepository.saveAndFlush(
                new Cinema(
                        "CGV Vincom",
                        "72 Le Thanh Ton",
                        "Ho Chi Minh"));

        room = roomRepository.saveAndFlush(
                new Room(
                        cinema,
                        "Room 01",
                        RoomType.STANDARD));
    }

    @Test
    void findAllByRoomIdOrderBySeatNumberAscShouldReturnAllSeats() {
        Seat a2 = seat("A2", "A", SeatType.STANDARD);
        Seat a1 = seat("A1", "A", SeatType.STANDARD);
        Seat b1 = seat("B1", "B", SeatType.VIP);

        b1.deactivate();

        seatRepository.saveAllAndFlush(
                List.of(a2, a1, b1));

        entityManager.clear();

        List<Seat> result = seatRepository
                .findAllByRoom_IdOrderBySeatNumberAsc(
                        room.getId());

        assertThat(result)
                .extracting(Seat::getSeatNumber)
                .containsExactly(
                        "A1",
                        "A2",
                        "B1");
    }

    @Test
    void findAllByRoomIdAndActiveTrueShouldExcludeInactiveSeats() {
        Seat a2 = seat("A2", "A", SeatType.STANDARD);
        Seat a1 = seat("A1", "A", SeatType.STANDARD);
        Seat b1 = seat("B1", "B", SeatType.VIP);

        b1.deactivate();

        seatRepository.saveAllAndFlush(
                List.of(a2, a1, b1));

        entityManager.clear();

        List<Seat> result = seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        room.getId());

        assertThat(result)
                .extracting(Seat::getSeatNumber)
                .containsExactly(
                        "A1",
                        "A2");

        assertThat(result).allMatch(Seat::isActive);
    }

    @Test
    void existsByRoomIdAndSeatNumberIgnoreCaseShouldIgnoreCase() {
        seatRepository.saveAndFlush(
                seat("A1", "A", SeatType.STANDARD));

        boolean exists = seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        room.getId(),
                        "a1");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByRoomIdAndSeatNumberShouldNotMatchAnotherRoom() {
        seatRepository.saveAndFlush(
                seat("A1", "A", SeatType.STANDARD));

        Room anotherRoom = roomRepository.saveAndFlush(
                new Room(
                        room.getCinema(),
                        "Room 02",
                        RoomType.IMAX));

        boolean exists = seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCase(
                        anotherRoom.getId(),
                        "A1");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByRoomIdAndSeatNumberAndIdNotShouldExcludeCurrentSeat() {
        Seat currentSeat = seatRepository.saveAndFlush(
                seat("A1", "A", SeatType.STANDARD));

        seatRepository.saveAndFlush(
                seat("A2", "A", SeatType.STANDARD));

        boolean currentSeatExists = seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        room.getId(),
                        "a1",
                        currentSeat.getId());

        boolean anotherSeatExists = seatRepository
                .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        room.getId(),
                        "a2",
                        currentSeat.getId());

        assertThat(currentSeatExists).isFalse();
        assertThat(anotherSeatExists).isTrue();
    }

    private Seat seat(
            String seatNumber,
            String rowLabel,
            SeatType seatType) {

        return new Seat(
                room,
                seatNumber,
                rowLabel,
                seatType);
    }
}
