package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowSeatService;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ShowSeatConcurrencyIntegrationTest {

    private static final BigDecimal PRICE = new BigDecimal("120000.00");

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.0")
            .withDatabaseName(
                    "inventory_concurrency_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ShowSeatService showSeatService;

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

    private ExecutorService executorService;
    private UUID showSeatId;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(2);

        Cinema cinema = cinemaRepository.saveAndFlush(
                new Cinema(
                        "CGV Concurrency",
                        "72 Le Thanh Ton",
                        "Ho Chi Minh"));

        Room room = roomRepository.saveAndFlush(
                new Room(
                        cinema,
                        "Room Concurrency",
                        RoomType.STANDARD));

        Seat seat = seatRepository.saveAndFlush(
                new Seat(
                        room,
                        "A1",
                        "A",
                        SeatType.STANDARD));

        OffsetDateTime startsAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(1);

        Showtime showtime = showtimeRepository.saveAndFlush(
                new Showtime(
                        UUID.randomUUID(),
                        room,
                        startsAt,
                        startsAt.plusHours(2)));

        ShowSeat showSeat = showSeatRepository.saveAndFlush(
                new ShowSeat(
                        showtime,
                        seat,
                        PRICE));

        showSeatId = showSeat.getId();
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executorService.shutdown();

        if (!executorService.awaitTermination(
                10,
                TimeUnit.SECONDS)) {

            executorService.shutdownNow();
        }
    }

    @Test
    void concurrentHoldShouldAllowOnlyOneBooking()
            throws Exception {

        UUID bookingA = UUID.randomUUID();
        UUID bookingB = UUID.randomUUID();

        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusMinutes(10);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<HoldResult> firstResult = executorService.submit(() -> hold(
                bookingA,
                expiresAt,
                ready,
                start));

        Future<HoldResult> secondResult = executorService.submit(() -> hold(
                bookingB,
                expiresAt,
                ready,
                start));

        assertThat(
                ready.await(
                        10,
                        TimeUnit.SECONDS))
                .isTrue();

        start.countDown();

        HoldResult resultA = firstResult.get(
                20,
                TimeUnit.SECONDS);

        HoldResult resultB = secondResult.get(
                20,
                TimeUnit.SECONDS);

        assertThat(
                java.util.List.of(
                        resultA,
                        resultB))
                .containsExactlyInAnyOrder(
                        HoldResult.SUCCESS,
                        HoldResult.CONFLICT);

        ShowSeat persisted = showSeatRepository
                .findById(showSeatId)
                .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(persisted.getHeldByBookingId())
                .isIn(bookingA, bookingB);

        assertThat(persisted.getHoldExpiresAt())
                .isNotNull();
    }

    private HoldResult hold(
            UUID bookingId,
            OffsetDateTime expiresAt,
            CountDownLatch ready,
            CountDownLatch start)
            throws InterruptedException {

        ready.countDown();

        if (!start.await(
                10,
                TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Concurrent requests did not start");
        }

        try {
            showSeatService.hold(
                    showSeatId,
                    new HoldShowSeatRequest(
                            bookingId,
                            expiresAt));

            return HoldResult.SUCCESS;
        } catch (ConflictException exception) {
            return HoldResult.CONFLICT;
        }
    }

    private enum HoldResult {
        SUCCESS,
        CONFLICT
    }
}
