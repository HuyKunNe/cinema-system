package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.RequestedSeatPayload;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.SeatReservationRequestedConsumerService;
import com.cinema.inventory.service.SeatReservationRequestedConsumerService.Status;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Import(SeatReservationRequestedIntegrationTest.FixedClockConfiguration.class)
class SeatReservationRequestedIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-08-21T10:00:00Z");

    private static final OffsetDateTime NOW =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    @Autowired private SeatReservationRequestedConsumerService consumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    private ExecutorService executorService;

    private UUID showtimeId;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        executorService = Executors.newFixedThreadPool(2);

        createInventory();
    }

    @AfterEach
    void tearDown() throws InterruptedException {

        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {

            executorService.shutdownNow();
        }
    }

    @Test
    void completeAvailableSeatSetShouldBeHeldAtomically() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId, List.of("H8", "H7"));

        SeatReservationRequestedConsumerService.Result result =
                consumerService.handle(bookingId.toString(), message);

        assertThat(result.status()).isEqualTo(Status.RESERVED);

        assertThat(result.outboxEventId()).isNotNull();

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId()).isEqualTo(bookingId);

                            assertThat(showSeat.getHoldExpiresAt()).isEqualTo(NOW.plusMinutes(10));
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);

        OutboxEventEntity outboxEvent =
                outboxRepository.findById(result.outboxEventId()).orElseThrow();

        assertThat(outboxEvent.getEventType()).isEqualTo(InventoryEventContract.SEAT_RESERVED);

        assertThat(outboxEvent.getAggregateId()).isEqualTo(bookingId);

        assertThat(outboxEvent.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(outboxEvent.getCorrelationId()).isEqualTo(message.correlationId());

        assertThat(outboxEvent.getCausationId()).isEqualTo(message.eventId());

        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void missingSeatShouldRejectWithoutPartialHold() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId, List.of("H7", "H9"));

        SeatReservationRequestedConsumerService.Result result =
                consumerService.handle(bookingId.toString(), message);

        assertThat(result.status()).isEqualTo(Status.REJECTED);

        List<ShowSeat> persistedSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        assertThat(persistedSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);

        OutboxEventEntity rejection =
                outboxRepository.findById(result.outboxEventId()).orElseThrow();

        assertThat(rejection.getEventType())
                .isEqualTo(InventoryEventContract.SEAT_RESERVATION_REJECTED);

        assertThat(rejection.getPayload())
                .contains("\"reasonCode\":\"SEAT_NOT_FOUND\"")
                .contains("\"H9\"");
    }

    @Test
    void duplicateEventShouldNotHoldAgainOrCreateAnotherOutbox() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = message(bookingId, List.of("H7", "H8"));

        SeatReservationRequestedConsumerService.Result first =
                consumerService.handle(bookingId.toString(), message);

        SeatReservationRequestedConsumerService.Result duplicate =
                consumerService.handle(bookingId.toString(), message);

        assertThat(first.status()).isEqualTo(Status.RESERVED);

        assertThat(duplicate.status()).isEqualTo(Status.DUPLICATE);

        assertThat(duplicate.outboxEventId()).isNull();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(1);

        assertThat(showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId))
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD);

                            assertThat(showSeat.getHeldByBookingId()).isEqualTo(bookingId);
                        });
    }

    @Test
    void concurrentBookingsForSameSeatsShouldHaveOneWinner() throws Exception {

        UUID bookingA = UuidGenerator.next();
        UUID bookingB = UuidGenerator.next();

        OutboxEventMessage messageA = message(bookingA, List.of("H7", "H8"));

        OutboxEventMessage messageB = message(bookingB, List.of("H8", "H7"));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Status> resultA =
                executorService.submit(() -> handleConcurrently(bookingA, messageA, ready, start));

        Future<Status> resultB =
                executorService.submit(() -> handleConcurrently(bookingB, messageB, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        assertThat(List.of(resultA.get(20, TimeUnit.SECONDS), resultB.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(Status.RESERVED, Status.REJECTED);

        List<ShowSeat> persistedSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtimeId);

        assertThat(persistedSeats)
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.HELD));

        UUID winningBookingId = persistedSeats.getFirst().getHeldByBookingId();

        assertThat(winningBookingId).isIn(bookingA, bookingB);

        assertThat(persistedSeats)
                .allSatisfy(
                        showSeat ->
                                assertThat(showSeat.getHeldByBookingId())
                                        .isEqualTo(winningBookingId));

        assertThat(processedEventRepository.count()).isEqualTo(2);

        assertThat(outboxRepository.count()).isEqualTo(2);

        assertThat(
                        outboxRepository.findAll().stream()
                                .map(OutboxEventEntity::getEventType)
                                .toList())
                .containsExactlyInAnyOrder(
                        InventoryEventContract.SEAT_RESERVED,
                        InventoryEventContract.SEAT_RESERVATION_REJECTED);
    }

    private Status handleConcurrently(
            UUID bookingId, OutboxEventMessage message, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException("Concurrent requests did not start");
        }

        return consumerService.handle(bookingId.toString(), message).status();
    }

    private OutboxEventMessage message(UUID bookingId, List<String> seatNumbers) {

        SeatReservationRequestedPayload payload =
                new SeatReservationRequestedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        showtimeId,
                        seatNumbers.stream().map(RequestedSeatPayload::new).toList(),
                        NOW,
                        NOW.plusMinutes(10));

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                InventoryEventContract.SEAT_RESERVATION_REQUESTED,
                InventoryEventContract.SEAT_RESERVATION_REQUESTED_VERSION,
                NOW,
                "booking-service",
                UuidGenerator.next(),
                null,
                objectMapper.valueToTree(payload));
    }

    private void createInventory() {

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Cinema Reservation Test", "123 Main Street", "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Room Reservation", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(), room, NOW.plusDays(1), NOW.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        showtimeId = savedShowtime.getId();

        showSeatRepository.saveAllAndFlush(
                List.of(
                        new ShowSeat(savedShowtime, h7, STANDARD_PRICE),
                        new ShowSeat(savedShowtime, h8, VIP_PRICE)));
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        showSeatRepository.deleteAll();
        showtimeRepository.deleteAll();
        seatRepository.deleteAll();
        roomRepository.deleteAll();
        cinemaRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedInventoryTestClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
