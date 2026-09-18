package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
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
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.BookingConfirmedConsumerService;
import com.cinema.inventory.service.SeatReleaseRequestedConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Import(InventoryConfirmationReleaseRaceIntegrationTest.FixedClockConfiguration.class)
class InventoryConfirmationReleaseRaceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-18T10:01:00Z");

    private static final OffsetDateTime CURRENT_TIME =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final OffsetDateTime EVENT_TIME = OffsetDateTime.parse("2026-09-18T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-18T10:10:00Z");

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    @Autowired private BookingConfirmedConsumerService bookingConfirmedConsumerService;

    @Autowired private SeatReleaseRequestedConsumerService seatReleaseRequestedConsumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        context = createHeldInventory();
    }

    @AfterEach
    void tearDown() {

        cleanDatabase();
    }

    @Test
    void delayedReleaseShouldNotReverseBookedSeats() {

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage confirmation =
                bookingConfirmedMessage(UuidGenerator.next(), correlationId);

        OutboxEventMessage delayedRelease =
                seatReleaseRequestedMessage(UuidGenerator.next(), correlationId);

        BookingConfirmedConsumerService.Result confirmationResult =
                bookingConfirmedConsumerService.handle(
                        context.bookingId().toString(), confirmation);

        assertThatThrownBy(
                        () ->
                                seatReleaseRequestedConsumerService.handle(
                                        context.bookingId().toString(), delayedRelease))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        assertThat(confirmationResult.status())
                .isEqualTo(BookingConfirmedConsumerService.Status.BOOKED);

        assertBooked();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                confirmation.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedRelease.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isFalse();

        /*
         * booking-confirmed is terminal inside Inventory and does not
         * emit another event.
         *
         * The rejected delayed release must not emit seat-released.
         */
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void delayedConfirmationShouldNotReverseReleasedSeats() {

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage release =
                seatReleaseRequestedMessage(UuidGenerator.next(), correlationId);

        OutboxEventMessage delayedConfirmation =
                bookingConfirmedMessage(UuidGenerator.next(), correlationId);

        SeatReleaseRequestedConsumerService.Result releaseResult =
                seatReleaseRequestedConsumerService.handle(context.bookingId().toString(), release);

        assertThatThrownBy(
                        () ->
                                bookingConfirmedConsumerService.handle(
                                        context.bookingId().toString(), delayedConfirmation))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        assertThat(releaseResult.status())
                .isEqualTo(SeatReleaseRequestedConsumerService.Status.RELEASED);

        assertAvailable();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                release.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedConfirmation.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(event -> assertSeatReleasedOutbox(event, release));
    }

    @Test
    void concurrentConfirmationAndReleaseShouldHaveExactlyOneWinner() throws Exception {

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage confirmation =
                bookingConfirmedMessage(UuidGenerator.next(), correlationId);

        OutboxEventMessage release =
                seatReleaseRequestedMessage(UuidGenerator.next(), correlationId);

        ConcurrentResults results =
                executeConcurrently(
                        () ->
                                bookingConfirmedConsumerService.handle(
                                        context.bookingId().toString(), confirmation),
                        () ->
                                seatReleaseRequestedConsumerService.handle(
                                        context.bookingId().toString(), release));

        assertThat(results.values()).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        List<ShowSeat> seats = loadShowSeats();

        assertThat(seats).hasSize(2);

        boolean allBooked =
                seats.stream().allMatch(seat -> seat.getStatus() == ShowSeatStatus.BOOKED);

        boolean allAvailable =
                seats.stream().allMatch(seat -> seat.getStatus() == ShowSeatStatus.AVAILABLE);

        /*
         * The complete seat set must converge atomically.
         *
         * Mixed terminal state such as:
         *
         * H7 = BOOKED
         * H8 = AVAILABLE
         *
         * is never valid.
         */
        assertThat(allBooked || allAvailable).isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        if (allBooked) {

            assertConfirmedWinner(results, confirmation, release);

        } else {

            assertReleaseWinner(results, confirmation, release);
        }
    }

    private void assertConfirmedWinner(
            ConcurrentResults results,
            OutboxEventMessage confirmation,
            OutboxEventMessage release) {

        assertThat(results.values())
                .filteredOn(BookingConfirmedConsumerService.Result.class::isInstance)
                .hasSize(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                confirmation.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                release.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isFalse();

        /*
         * Confirmation wins:
         * HELD -> BOOKED
         *
         * There is no Inventory successor event for booking-confirmed.
         */
        assertThat(outboxRepository.count()).isZero();
    }

    private void assertReleaseWinner(
            ConcurrentResults results,
            OutboxEventMessage confirmation,
            OutboxEventMessage release) {

        assertThat(results.values())
                .filteredOn(SeatReleaseRequestedConsumerService.Result.class::isInstance)
                .hasSize(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                release.eventId(),
                                InventoryEventContract.SEAT_RELEASE_REQUESTED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                confirmation.eventId(),
                                InventoryEventContract.BOOKING_CONFIRMED_CONSUMER))
                .isFalse();

        /*
         * Release wins:
         * HELD -> AVAILABLE
         *
         * Exactly one canonical seat-released event is emitted.
         */
        assertThat(outboxRepository.findAll())
                .singleElement()
                .satisfies(event -> assertSeatReleasedOutbox(event, release));
    }

    private ConcurrentResults executeConcurrently(
            Callable<Object> firstTask, Callable<Object> secondTask) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {

            Future<Object> first = executor.submit(concurrentTask(firstTask, ready, start));

            Future<Object> second = executor.submit(concurrentTask(secondTask, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            return new ConcurrentResults(
                    first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

        } finally {

            executor.shutdownNow();

            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    private Callable<Object> concurrentTask(
            Callable<Object> task, CountDownLatch ready, CountDownLatch start) {

        return () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return new IllegalStateException("Timed out waiting for concurrent Inventory race");
            }

            try {

                return task.call();

            } catch (RuntimeException exception) {

                return exception;
            }
        };
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema("Inventory Race Test", "123 Main Street", "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(
                        new Room(cinema, "Inventory Race Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        CURRENT_TIME.plusDays(1),
                        CURRENT_TIME.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat first = new ShowSeat(savedShowtime, h7, STANDARD_PRICE);

        first.hold(bookingId, HOLD_EXPIRES_AT, EVENT_TIME.minusMinutes(1));

        ShowSeat second = new ShowSeat(savedShowtime, h8, VIP_PRICE);

        second.hold(bookingId, HOLD_EXPIRES_AT, EVENT_TIME.minusMinutes(1));

        List<ShowSeat> savedSeats = showSeatRepository.saveAllAndFlush(List.of(first, second));

        List<UUID> showSeatIds =
                savedSeats.stream().map(ShowSeat::getId).sorted(Comparator.naturalOrder()).toList();

        entityManager.clear();

        return new TestContext(bookingId, userId, paymentId, savedShowtime.getId(), showSeatIds);
    }

    private OutboxEventMessage bookingConfirmedMessage(UUID eventId, UUID correlationId) {

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        context.bookingId(),
                        context.userId(),
                        context.showtimeId(),
                        context.paymentId(),
                        List.of(
                                new ConfirmedSeatPayload("H7", "STANDARD", STANDARD_PRICE),
                                new ConfirmedSeatPayload("H8", "VIP", VIP_PRICE)),
                        TOTAL_AMOUNT,
                        InventoryEventContract.CURRENCY_VND,
                        EVENT_TIME);

        return new OutboxEventMessage(
                eventId,
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                EVENT_TIME,
                InventoryEventContract.BOOKING_PRODUCER,
                correlationId,
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage seatReleaseRequestedMessage(UUID eventId, UUID correlationId) {

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        context.showSeatIds(),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        EVENT_TIME);

        return new OutboxEventMessage(
                eventId,
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                EVENT_TIME,
                InventoryEventContract.BOOKING_PRODUCER,
                correlationId,
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private List<ShowSeat> loadShowSeats() {

        return showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());
    }

    private void assertBooked() {

        assertThat(loadShowSeats())
                .hasSize(2)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.BOOKED);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });
    }

    private void assertAvailable() {

        assertThat(loadShowSeats())
                .hasSize(2)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });
    }

    private void assertSeatReleasedOutbox(
            OutboxEventEntity event, OutboxEventMessage sourceMessage) {

        assertThat(event.getEventType()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(event.getEventVersion()).isEqualTo(InventoryEventContract.SEAT_RELEASED_VERSION);

        assertThat(event.getTopic()).isEqualTo(InventoryEventContract.SEAT_RELEASED);

        assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(event.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(sourceMessage.eventId());
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        showSeatRepository.deleteAllInBatch();

        showtimeRepository.deleteAllInBatch();

        seatRepository.deleteAllInBatch();

        roomRepository.deleteAllInBatch();

        cinemaRepository.deleteAllInBatch();
    }

    private record TestContext(
            UUID bookingId, UUID userId, UUID paymentId, UUID showtimeId, List<UUID> showSeatIds) {}

    private record ConcurrentResults(Object first, Object second) {

        List<Object> values() {

            return List.of(first, second);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock inventoryRaceClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
