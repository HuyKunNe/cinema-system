package com.cinema.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class ShowtimeRepositoryTest {

    private static final OffsetDateTime BASE_TIME = OffsetDateTime.parse("2099-01-01T03:00:00Z");

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private EntityManager entityManager;

    private Room room;
    private UUID movieId;

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

        movieId = UUID.randomUUID();
    }

    @Test
    void findAllByRoomIdOrderByStartsAtAscShouldReturnSortedShowtimes() {
        Showtime later = showtime(
                movieId,
                room,
                BASE_TIME.plusHours(4),
                BASE_TIME.plusHours(6));

        Showtime earlier = showtime(
                UUID.randomUUID(),
                room,
                BASE_TIME,
                BASE_TIME.plusHours(2));

        showtimeRepository.saveAllAndFlush(
                List.of(later, earlier));

        entityManager.clear();

        List<Showtime> result = showtimeRepository
                .findAllByRoom_IdOrderByStartsAtAsc(
                        room.getId());

        assertThat(result)
                .extracting(Showtime::getStartsAt)
                .containsExactly(
                        BASE_TIME,
                        BASE_TIME.plusHours(4));
    }

    @Test
    void findAllByMovieIdOrderByStartsAtAscShouldFilterByMovie() {
        Showtime later = showtime(
                movieId,
                room,
                BASE_TIME.plusHours(4),
                BASE_TIME.plusHours(6));

        Showtime earlier = showtime(
                movieId,
                room,
                BASE_TIME,
                BASE_TIME.plusHours(2));

        Showtime anotherMovie = showtime(
                UUID.randomUUID(),
                room,
                BASE_TIME.plusHours(8),
                BASE_TIME.plusHours(10));

        showtimeRepository.saveAllAndFlush(
                List.of(later, earlier, anotherMovie));

        entityManager.clear();

        List<Showtime> result = showtimeRepository
                .findAllByMovieIdOrderByStartsAtAsc(movieId);

        assertThat(result)
                .extracting(Showtime::getStartsAt)
                .containsExactly(
                        BASE_TIME,
                        BASE_TIME.plusHours(4));

        assertThat(result)
                .allMatch(showtime -> movieId.equals(
                        showtime.getMovieId()));
    }

    @Test
    void findAllByStartsAtBetweenShouldReturnShowtimesInsideRange() {
        Showtime beforeRange = showtime(
                movieId,
                room,
                BASE_TIME.minusHours(4),
                BASE_TIME.minusHours(2));

        Showtime insideRange = showtime(
                movieId,
                room,
                BASE_TIME.plusHours(1),
                BASE_TIME.plusHours(3));

        Showtime afterRange = showtime(
                movieId,
                room,
                BASE_TIME.plusHours(8),
                BASE_TIME.plusHours(10));

        showtimeRepository.saveAllAndFlush(
                List.of(
                        beforeRange,
                        insideRange,
                        afterRange));

        entityManager.clear();

        List<Showtime> result = showtimeRepository
                .findAllByStartsAtBetweenOrderByStartsAtAsc(
                        BASE_TIME,
                        BASE_TIME.plusHours(6));

        assertThat(result)
                .extracting(Showtime::getId)
                .containsExactly(insideRange.getId());
    }

    @Test
    void existsOverlappingShowtimeShouldReturnTrueForOverlappingRange() {
        showtimeRepository.saveAndFlush(
                showtime(
                        movieId,
                        room,
                        BASE_TIME,
                        BASE_TIME.plusHours(2)));

        boolean exists = showtimeRepository.existsOverlappingShowtime(
                room.getId(),
                BASE_TIME.plusHours(1),
                BASE_TIME.plusHours(3),
                Set.of(
                        ShowtimeStatus.CANCELLED,
                        ShowtimeStatus.COMPLETED));

        assertThat(exists).isTrue();
    }

    @Test
    void existsOverlappingShowtimeShouldReturnFalseForAdjacentRange() {
        showtimeRepository.saveAndFlush(
                showtime(
                        movieId,
                        room,
                        BASE_TIME,
                        BASE_TIME.plusHours(2)));

        boolean exists = showtimeRepository.existsOverlappingShowtime(
                room.getId(),
                BASE_TIME.plusHours(2),
                BASE_TIME.plusHours(4),
                Set.of(
                        ShowtimeStatus.CANCELLED,
                        ShowtimeStatus.COMPLETED));

        assertThat(exists).isFalse();
    }

    @Test
    void existsOverlappingShowtimeShouldIgnoreExcludedStatus() {
        Showtime cancelled = showtime(
                movieId,
                room,
                BASE_TIME,
                BASE_TIME.plusHours(2));

        cancelled.cancel();

        showtimeRepository.saveAndFlush(cancelled);

        boolean exists = showtimeRepository.existsOverlappingShowtime(
                room.getId(),
                BASE_TIME.plusHours(1),
                BASE_TIME.plusHours(3),
                Set.of(
                        ShowtimeStatus.CANCELLED,
                        ShowtimeStatus.COMPLETED));

        assertThat(exists).isFalse();
    }

    @Test
    void existsOverlappingShowtimeExcludingIdShouldExcludeCurrentShowtime() {
        Showtime current = showtimeRepository.saveAndFlush(
                showtime(
                        movieId,
                        room,
                        BASE_TIME,
                        BASE_TIME.plusHours(2)));

        boolean exists = showtimeRepository
                .existsOverlappingShowtimeExcludingId(
                        room.getId(),
                        current.getId(),
                        BASE_TIME.plusMinutes(30),
                        BASE_TIME.plusHours(3),
                        Set.of(
                                ShowtimeStatus.CANCELLED,
                                ShowtimeStatus.COMPLETED));

        assertThat(exists).isFalse();
    }

    @Test
    void existsOverlappingShowtimeExcludingIdShouldFindAnotherShowtime() {
        Showtime current = showtimeRepository.saveAndFlush(
                showtime(
                        movieId,
                        room,
                        BASE_TIME,
                        BASE_TIME.plusHours(2)));

        showtimeRepository.saveAndFlush(
                showtime(
                        UUID.randomUUID(),
                        room,
                        BASE_TIME.plusHours(3),
                        BASE_TIME.plusHours(5)));

        boolean exists = showtimeRepository
                .existsOverlappingShowtimeExcludingId(
                        room.getId(),
                        current.getId(),
                        BASE_TIME.plusHours(4),
                        BASE_TIME.plusHours(6),
                        Set.of(
                                ShowtimeStatus.CANCELLED,
                                ShowtimeStatus.COMPLETED));

        assertThat(exists).isTrue();
    }

    private Showtime showtime(
            UUID movieId,
            Room showtimeRoom,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        return new Showtime(
                movieId,
                showtimeRoom,
                startsAt,
                endsAt);
    }
}
