package com.cinema.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class ShowSeatRepositoryTest {

    private static final OffsetDateTime SHOWTIME_START = OffsetDateTime.parse("2099-01-01T03:00:00Z");

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("120000.00");

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

    private Room room;
    private Showtime showtime;

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

        showtime = showtimeRepository.saveAndFlush(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        SHOWTIME_START,
                        SHOWTIME_START.plusHours(2)));
    }

    @Test
    void existsByShowtimeIdShouldReturnTrueWhenShowSeatsExist() {
        Seat seat = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        showSeatRepository.saveAndFlush(
                showSeat(seat));

        boolean exists = showSeatRepository.existsByShowtime_Id(
                showtime.getId());

        assertThat(exists).isTrue();
    }

    @Test
    void findAllByShowtimeIdOrderBySeatNumberAscShouldReturnSortedSeats() {
        Seat a2 = saveSeat(
                "A2",
                "A",
                SeatType.STANDARD);

        Seat a1 = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        Seat b1 = saveSeat(
                "B1",
                "B",
                SeatType.VIP);

        showSeatRepository.saveAllAndFlush(
                List.of(
                        showSeat(a2),
                        showSeat(a1),
                        showSeat(b1)));

        entityManager.clear();

        List<ShowSeat> result = showSeatRepository
                .findAllByShowtime_IdOrderBySeatNumberAsc(
                        showtime.getId());

        assertThat(result)
                .extracting(ShowSeat::getSeatNumber)
                .containsExactly(
                        "A1",
                        "A2",
                        "B1");
    }

    @Test
    void findAllByShowtimeIdAndStatusShouldFilterByStatus() {
        OffsetDateTime now = OffsetDateTime.parse("2099-01-01T00:00:00Z");

        Seat a1 = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        Seat a2 = saveSeat(
                "A2",
                "A",
                SeatType.STANDARD);

        ShowSeat available = showSeat(a1);
        ShowSeat held = showSeat(a2);

        held.hold(
                UUID.randomUUID(),
                now.plusMinutes(5),
                now);

        showSeatRepository.saveAllAndFlush(
                List.of(available, held));

        entityManager.clear();

        List<ShowSeat> result = showSeatRepository
                .findAllByShowtime_IdAndStatusOrderBySeatNumberAsc(
                        showtime.getId(),
                        ShowSeatStatus.HELD);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(showSeat -> {
                    assertThat(showSeat.getSeatNumber())
                            .isEqualTo("A2");
                    assertThat(showSeat.getStatus())
                            .isEqualTo(ShowSeatStatus.HELD);
                });
    }

    @Test
    void findByShowtimeIdAndSeatNumberIgnoreCaseShouldIgnoreCase() {
        Seat seat = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        ShowSeat saved = showSeatRepository.saveAndFlush(
                showSeat(seat));

        entityManager.clear();

        assertThat(
                showSeatRepository
                        .findByShowtime_IdAndSeatNumberIgnoreCase(
                                showtime.getId(),
                                "a1"))
                .isPresent()
                .get()
                .extracting(ShowSeat::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    void existsByShowtimeIdAndSeatIdShouldReturnExpectedResult() {
        Seat existingSeat = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        Seat missingSeat = saveSeat(
                "A2",
                "A",
                SeatType.STANDARD);

        showSeatRepository.saveAndFlush(
                showSeat(existingSeat));

        assertThat(
                showSeatRepository.existsByShowtime_IdAndSeat_Id(
                        showtime.getId(),
                        existingSeat.getId()))
                .isTrue();

        assertThat(
                showSeatRepository.existsByShowtime_IdAndSeat_Id(
                        showtime.getId(),
                        missingSeat.getId()))
                .isFalse();
    }

    @Test
    void findAllByShowtimeIdAndSeatNumbersForUpdateShouldReturnRequestedSeats() {
        Seat a1 = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        Seat a2 = saveSeat(
                "A2",
                "A",
                SeatType.STANDARD);

        Seat a3 = saveSeat(
                "A3",
                "A",
                SeatType.STANDARD);

        showSeatRepository.saveAllAndFlush(
                List.of(
                        showSeat(a1),
                        showSeat(a2),
                        showSeat(a3)));

        entityManager.clear();

        List<ShowSeat> result = showSeatRepository
                .findAllByShowtimeIdAndSeatNumbersForUpdate(
                        showtime.getId(),
                        List.of("A2", "A1"));

        assertThat(result)
                .extracting(ShowSeat::getSeatNumber)
                .containsExactly(
                        "A1",
                        "A2");
    }

    @Test
    void findByIdForUpdateShouldReturnShowSeat() {
        Seat seat = saveSeat(
                "B5",
                "B",
                SeatType.VIP);

        ShowSeat saved = showSeatRepository.saveAndFlush(
                showSeat(seat));

        entityManager.clear();

        assertThat(
                showSeatRepository.findByIdForUpdate(
                        saved.getId()))
                .isPresent()
                .get()
                .extracting(ShowSeat::getSeatNumber)
                .isEqualTo("B5");
    }

    @Test
    void findExpiredHoldsShouldReturnOnlyExpiredHeldSeats() {
        OffsetDateTime now = OffsetDateTime.parse("2099-01-01T01:00:00Z");

        Seat expiredSeat = saveSeat(
                "A1",
                "A",
                SeatType.STANDARD);

        Seat activeHoldSeat = saveSeat(
                "A2",
                "A",
                SeatType.STANDARD);

        Seat availableSeat = saveSeat(
                "A3",
                "A",
                SeatType.STANDARD);

        ShowSeat expiredHold = showSeat(expiredSeat);
        expiredHold.hold(
                UUID.randomUUID(),
                now.minusMinutes(1),
                now.minusMinutes(10));

        ShowSeat activeHold = showSeat(activeHoldSeat);
        activeHold.hold(
                UUID.randomUUID(),
                now.plusMinutes(5),
                now.minusMinutes(1));

        ShowSeat available = showSeat(availableSeat);

        showSeatRepository.saveAllAndFlush(
                List.of(
                        expiredHold,
                        activeHold,
                        available));

        entityManager.clear();

        List<ShowSeat> result = showSeatRepository.findExpiredHolds(
                ShowSeatStatus.HELD,
                now);

        assertThat(result)
                .hasSize(1)
                .first()
                .satisfies(showSeat -> {
                    assertThat(showSeat.getSeatNumber())
                            .isEqualTo("A1");
                    assertThat(showSeat.getStatus())
                            .isEqualTo(ShowSeatStatus.HELD);
                    assertThat(showSeat.getHoldExpiresAt())
                            .isBeforeOrEqualTo(now);
                });
    }

    @Test
    void lifecycleCandidateLookupShouldFilterByShowtimeAndBooking() {
        UUID bookingId = UUID.randomUUID();
        OffsetDateTime now = SHOWTIME_START.minusHours(1);
        Showtime otherShowtime = showtimeRepository.saveAndFlush(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        SHOWTIME_START.plusDays(1),
                        SHOWTIME_START.plusDays(1).plusHours(2)));
        ShowSeat first = showSeat(saveSeat("C1", "C", SeatType.STANDARD));
        ShowSeat second = showSeat(saveSeat("C2", "C", SeatType.STANDARD));
        ShowSeat otherBooking = showSeat(saveSeat("C3", "C", SeatType.STANDARD));
        ShowSeat otherShow = new ShowSeat(otherShowtime, first.getSeat(), DEFAULT_PRICE);
        first.hold(bookingId, now.plusMinutes(5), now);
        second.hold(bookingId, now.plusMinutes(5), now);
        otherBooking.hold(UUID.randomUUID(), now.plusMinutes(5), now);
        otherShow.hold(bookingId, now.plusMinutes(5), now);
        showSeatRepository.saveAllAndFlush(List.of(first, second, otherBooking, otherShow));
        entityManager.clear();

        List<UUID> expected = List.of(first.getId(), second.getId()).stream().sorted().toList();

        assertThat(showSeatRepository.findIdsByShowtimeIdAndBookingId(showtime.getId(), bookingId))
                .containsExactlyElementsOf(expected);
    }

    @Test
    void lifecycleLockLookupShouldKeepStaleCandidatesAndExcludeOtherShowtimes() {
        UUID bookingId = UUID.randomUUID();
        OffsetDateTime now = SHOWTIME_START.minusHours(1);
        ShowSeat booked = showSeat(saveSeat("D1", "D", SeatType.STANDARD));
        booked.hold(bookingId, now.plusMinutes(5), now);
        booked.book(bookingId);
        ShowSeat available = showSeat(saveSeat("D2", "D", SeatType.STANDARD));
        ShowSeat differentHold = showSeat(saveSeat("D3", "D", SeatType.STANDARD));
        differentHold.hold(UUID.randomUUID(), now.plusMinutes(5), now);
        ShowSeat unrequested = showSeat(saveSeat("D4", "D", SeatType.STANDARD));
        Showtime otherShowtime = showtimeRepository.saveAndFlush(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        SHOWTIME_START.plusDays(1),
                        SHOWTIME_START.plusDays(1).plusHours(2)));
        ShowSeat otherShow = new ShowSeat(otherShowtime, booked.getSeat(), DEFAULT_PRICE);
        showSeatRepository.saveAllAndFlush(
                List.of(booked, available, differentHold, unrequested, otherShow));
        entityManager.clear();

        List<UUID> expected =
                List.of(booked.getId(), available.getId(), differentHold.getId())
                        .stream().sorted().toList();
        List<ShowSeat> locked = showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                showtime.getId(),
                List.of(
                        differentHold.getId(),
                        otherShow.getId(),
                        available.getId(),
                        booked.getId()));

        assertThat(locked).extracting(ShowSeat::getId).containsExactlyElementsOf(expected);
        assertThat(locked)
                .extracting(ShowSeat::getStatus)
                .containsExactlyInAnyOrder(
                        ShowSeatStatus.BOOKED,
                        ShowSeatStatus.AVAILABLE,
                        ShowSeatStatus.HELD);
    }

    private Seat saveSeat(
            String seatNumber,
            String rowLabel,
            SeatType seatType) {

        return seatRepository.saveAndFlush(
                new Seat(
                        room,
                        seatNumber,
                        rowLabel,
                        seatType));
    }

    private ShowSeat showSeat(Seat seat) {
        return new ShowSeat(
                showtime,
                seat,
                DEFAULT_PRICE);
    }
}
