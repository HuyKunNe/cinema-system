package com.cinema.inventory.repository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.RoomType;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class RoomRepositoryTest {

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    private Cinema cinema;

    @BeforeEach
    void setUp() {
        cinema = cinemaRepository.saveAndFlush(
                new Cinema(
                        "CGV Vincom",
                        "72 Le Thanh Ton",
                        "Ho Chi Minh"));
    }

    @Test
    void findAllByCinemaIdOrderByNameAscShouldReturnAllRooms() {
        Room roomB = room("Room B", RoomType.IMAX);
        Room roomA = room("Room A", RoomType.STANDARD);
        Room inactiveRoom = room("Room C", RoomType.VIP);

        inactiveRoom.deactivate();

        roomRepository.saveAllAndFlush(
                List.of(roomB, roomA, inactiveRoom));

        entityManager.clear();

        List<Room> result = roomRepository.findAllByCinema_IdOrderByNameAsc(
                cinema.getId());

        assertThat(result)
                .extracting(Room::getName)
                .containsExactly(
                        "Room A",
                        "Room B",
                        "Room C");
    }

    @Test
    void findAllByCinemaIdAndActiveTrueShouldExcludeInactiveRooms() {
        Room activeB = room("Room B", RoomType.IMAX);
        Room activeA = room("Room A", RoomType.STANDARD);
        Room inactive = room("Room C", RoomType.VIP);

        inactive.deactivate();

        roomRepository.saveAllAndFlush(
                List.of(activeB, activeA, inactive));

        entityManager.clear();

        List<Room> result = roomRepository
                .findAllByCinema_IdAndActiveTrueOrderByNameAsc(
                        cinema.getId());

        assertThat(result)
                .extracting(Room::getName)
                .containsExactly(
                        "Room A",
                        "Room B");

        assertThat(result).allMatch(Room::isActive);
    }

    @Test
    void existsByCinemaIdAndNameIgnoreCaseShouldIgnoreCase() {
        roomRepository.saveAndFlush(
                room("Premium Room", RoomType.VIP));

        boolean exists = roomRepository.existsByCinema_IdAndNameIgnoreCase(
                cinema.getId(),
                "PREMIUM ROOM");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByCinemaIdAndNameIgnoreCaseShouldNotMatchAnotherCinema() {
        roomRepository.saveAndFlush(
                room("Room 01", RoomType.STANDARD));

        Cinema anotherCinema = cinemaRepository.saveAndFlush(
                new Cinema(
                        "Lotte Cinema",
                        "469 Nguyen Huu Tho",
                        "Ho Chi Minh"));

        boolean exists = roomRepository.existsByCinema_IdAndNameIgnoreCase(
                anotherCinema.getId(),
                "Room 01");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByCinemaIdAndNameIgnoreCaseAndIdNotShouldExcludeCurrentRoom() {
        Room currentRoom = roomRepository.saveAndFlush(
                room("Room 01", RoomType.STANDARD));

        boolean existsForCurrentRoom = roomRepository
                .existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                        cinema.getId(),
                        "ROOM 01",
                        currentRoom.getId());

        Room duplicate = roomRepository.saveAndFlush(
                room("Room 02", RoomType.IMAX));

        boolean existsForAnotherRoom = roomRepository
                .existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                        cinema.getId(),
                        "ROOM 02",
                        currentRoom.getId());

        assertThat(existsForCurrentRoom).isFalse();
        assertThat(existsForAnotherRoom).isTrue();
        assertThat(duplicate.getId()).isNotNull();
    }

    private Room room(
            String name,
            RoomType roomType) {

        return new Room(cinema, name, roomType);
    }
}
