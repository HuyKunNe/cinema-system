package com.cinema.inventory.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
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
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.ProcessedEventRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.BookingCancelledConsumerService;
import com.cinema.inventory.service.BookingExpiredConsumerService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class BookingLifecycleReleaseConsumerMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal STANDARD_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal VIP_PRICE = new BigDecimal("120000.00");

    private static final OffsetDateTime EVENT_AT =
            OffsetDateTime.of(2026, 9, 16, 10, 0, 0, 0, ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT = EVENT_AT.plusMinutes(10);

    @Autowired private BookingCancelledConsumerService bookingCancelledConsumerService;

    @Autowired private BookingExpiredConsumerService bookingExpiredConsumerService;

    @Autowired private CinemaRepository cinemaRepository;

    @Autowired private RoomRepository roomRepository;

    @Autowired private SeatRepository seatRepository;

    @Autowired private ShowtimeRepository showtimeRepository;

    @Autowired private ShowSeatRepository showSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    private ExecutorService executorService;

    private TestContext context;

    @BeforeEach
    void setUp() {

        cleanDatabase();

        executorService = Executors.newFixedThreadPool(2);

        context = createHeldInventory();
    }

    @AfterEach
    void tearDown() throws InterruptedException {

        executorService.shutdown();

        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {

            executorService.shutdownNow();
        }

        cleanDatabase();
    }

    @Test
    void canonicalCancellationShouldReleaseHeldSeats() {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        BookingCancelledConsumerService.Result result =
                bookingCancelledConsumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(result.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(),
                                InventoryEventContract.BOOKING_CANCELLED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateCancellationShouldReleaseOnlyOnce() {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        BookingCancelledConsumerService.Result first =
                bookingCancelledConsumerService.handle(context.bookingId().toString(), message);

        BookingCancelledConsumerService.Result duplicate =
                bookingCancelledConsumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        assertThat(duplicate.status()).isEqualTo(BookingCancelledConsumerService.Status.DUPLICATE);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void canonicalExpirationShouldReleaseHeldSeats() {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        BookingExpiredConsumerService.Result result =
                bookingExpiredConsumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(result.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), InventoryEventContract.BOOKING_EXPIRED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateExpirationShouldReleaseOnlyOnce() {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        BookingExpiredConsumerService.Result first =
                bookingExpiredConsumerService.handle(context.bookingId().toString(), message);

        BookingExpiredConsumerService.Result duplicate =
                bookingExpiredConsumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        assertThat(first.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        assertThat(duplicate.status()).isEqualTo(BookingExpiredConsumerService.Status.DUPLICATE);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void concurrentDuplicateCancellationShouldReleaseOnce() throws Exception {

        OutboxEventMessage message = bookingCancelledMessage(UuidGenerator.next());

        List<CancelledOutcome> outcomes = executeCancelledConcurrently(message, message);

        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failure()).isNull());

        assertThat(outcomes)
                .extracting(CancelledOutcome::status)
                .containsExactlyInAnyOrder(
                        BookingCancelledConsumerService.Status.RELEASED,
                        BookingCancelledConsumerService.Status.DUPLICATE);

        entityManager.clear();

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void concurrentDuplicateExpirationShouldReleaseOnce() throws Exception {

        OutboxEventMessage message = bookingExpiredMessage(UuidGenerator.next());

        List<ExpiredOutcome> outcomes = executeExpiredConcurrently(message, message);

        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failure()).isNull());

        assertThat(outcomes)
                .extracting(ExpiredOutcome::status)
                .containsExactlyInAnyOrder(
                        BookingExpiredConsumerService.Status.RELEASED,
                        BookingExpiredConsumerService.Status.DUPLICATE);

        entityManager.clear();

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void cancellationFollowedByExpirationShouldRemainReleased() {

        OutboxEventMessage cancelled = bookingCancelledMessage(UuidGenerator.next());

        OutboxEventMessage expired = bookingExpiredMessage(UuidGenerator.next());

        BookingCancelledConsumerService.Result cancelledResult =
                bookingCancelledConsumerService.handle(context.bookingId().toString(), cancelled);

        BookingExpiredConsumerService.Result expiredResult =
                bookingExpiredConsumerService.handle(context.bookingId().toString(), expired);

        entityManager.clear();

        assertThat(cancelledResult.status())
                .isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        assertThat(expiredResult.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(2);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                cancelled.eventId(),
                                InventoryEventContract.BOOKING_CANCELLED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                expired.eventId(), InventoryEventContract.BOOKING_EXPIRED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void expirationFollowedByCancellationShouldRemainReleased() {

        OutboxEventMessage expired = bookingExpiredMessage(UuidGenerator.next());

        OutboxEventMessage cancelled = bookingCancelledMessage(UuidGenerator.next());

        BookingExpiredConsumerService.Result expiredResult =
                bookingExpiredConsumerService.handle(context.bookingId().toString(), expired);

        BookingCancelledConsumerService.Result cancelledResult =
                bookingCancelledConsumerService.handle(context.bookingId().toString(), cancelled);

        entityManager.clear();

        assertThat(expiredResult.status()).isEqualTo(BookingExpiredConsumerService.Status.RELEASED);

        assertThat(cancelledResult.status())
                .isEqualTo(BookingCancelledConsumerService.Status.RELEASED);

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(2);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void concurrentCancellationAndExpirationShouldConvergeToReleased() throws Exception {

        OutboxEventMessage cancelled = bookingCancelledMessage(UuidGenerator.next());

        OutboxEventMessage expired = bookingExpiredMessage(UuidGenerator.next());

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<LifecycleOutcome> cancellationFuture =
                executorService.submit(() -> handleCancellation(cancelled, ready, start));

        Future<LifecycleOutcome> expirationFuture =
                executorService.submit(() -> handleExpiration(expired, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        LifecycleOutcome cancellation = cancellationFuture.get(30, TimeUnit.SECONDS);

        LifecycleOutcome expiration = expirationFuture.get(30, TimeUnit.SECONDS);

        assertThat(cancellation.failure()).isNull();

        assertThat(expiration.failure()).isNull();

        assertThat(cancellation.released()).isTrue();

        assertThat(expiration.released()).isTrue();

        entityManager.clear();

        assertReleased();

        assertThat(processedEventRepository.count()).isEqualTo(2);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                cancelled.eventId(),
                                InventoryEventContract.BOOKING_CANCELLED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                expired.eventId(), InventoryEventContract.BOOKING_EXPIRED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.count()).isZero();
    }

    private List<CancelledOutcome> executeCancelledConcurrently(
            OutboxEventMessage firstMessage, OutboxEventMessage secondMessage) throws Exception {

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<CancelledOutcome> first =
                executorService.submit(
                        () -> handleCancelledConcurrently(firstMessage, ready, start));

        Future<CancelledOutcome> second =
                executorService.submit(
                        () -> handleCancelledConcurrently(secondMessage, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }

    private CancelledOutcome handleCancelledConcurrently(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return CancelledOutcome.failure(
                        new IllegalStateException(
                                "Timed out waiting to start booking cancellation"));
            }

            BookingCancelledConsumerService.Result result =
                    bookingCancelledConsumerService.handle(context.bookingId().toString(), message);

            return CancelledOutcome.success(result.status());

        } catch (Throwable failure) {

            return CancelledOutcome.failure(failure);
        }
    }

    private List<ExpiredOutcome> executeExpiredConcurrently(
            OutboxEventMessage firstMessage, OutboxEventMessage secondMessage) throws Exception {

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        Future<ExpiredOutcome> first =
                executorService.submit(() -> handleExpiredConcurrently(firstMessage, ready, start));

        Future<ExpiredOutcome> second =
                executorService.submit(
                        () -> handleExpiredConcurrently(secondMessage, ready, start));

        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
    }

    private ExpiredOutcome handleExpiredConcurrently(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return ExpiredOutcome.failure(
                        new IllegalStateException("Timed out waiting to start booking expiration"));
            }

            BookingExpiredConsumerService.Result result =
                    bookingExpiredConsumerService.handle(context.bookingId().toString(), message);

            return ExpiredOutcome.success(result.status());

        } catch (Throwable failure) {

            return ExpiredOutcome.failure(failure);
        }
    }

    private LifecycleOutcome handleCancellation(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return LifecycleOutcome.failure(
                        new IllegalStateException(
                                "Timed out waiting to start booking cancellation"));
            }

            BookingCancelledConsumerService.Result result =
                    bookingCancelledConsumerService.handle(context.bookingId().toString(), message);

            return LifecycleOutcome.success(
                    result.status() == BookingCancelledConsumerService.Status.RELEASED);

        } catch (Throwable failure) {

            return LifecycleOutcome.failure(failure);
        }
    }

    private LifecycleOutcome handleExpiration(
            OutboxEventMessage message, CountDownLatch ready, CountDownLatch start) {

        try {

            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return LifecycleOutcome.failure(
                        new IllegalStateException("Timed out waiting to start booking expiration"));
            }

            BookingExpiredConsumerService.Result result =
                    bookingExpiredConsumerService.handle(context.bookingId().toString(), message);

            return LifecycleOutcome.success(
                    result.status() == BookingExpiredConsumerService.Status.RELEASED);

        } catch (Throwable failure) {

            return LifecycleOutcome.failure(failure);
        }
    }

    private TestContext createHeldInventory() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        Cinema cinema =
                cinemaRepository.saveAndFlush(
                        new Cinema(
                                "Booking Lifecycle Release Test",
                                "123 Main Street",
                                "Ho Chi Minh City"));

        Room room =
                roomRepository.saveAndFlush(new Room(cinema, "Lifecycle Room", RoomType.STANDARD));

        Seat h7 = seatRepository.saveAndFlush(new Seat(room, "H7", "H", SeatType.STANDARD));

        Seat h8 = seatRepository.saveAndFlush(new Seat(room, "H8", "H", SeatType.VIP));

        Showtime showtime =
                new Showtime(
                        UuidGenerator.next(),
                        room,
                        EVENT_AT.plusDays(1),
                        EVENT_AT.plusDays(1).plusHours(2));

        showtime.openForBooking();

        Showtime savedShowtime = showtimeRepository.saveAndFlush(showtime);

        ShowSeat first = new ShowSeat(savedShowtime, h7, STANDARD_PRICE);

        first.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        ShowSeat second = new ShowSeat(savedShowtime, h8, VIP_PRICE);

        second.hold(bookingId, HOLD_EXPIRES_AT, EVENT_AT.minusMinutes(1));

        showSeatRepository.saveAllAndFlush(List.of(first, second));

        entityManager.clear();

        return new TestContext(bookingId, userId, savedShowtime.getId());
    }

    private OutboxEventMessage bookingCancelledMessage(UUID eventId) {

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        context.bookingId(),
                        context.userId(),
                        context.showtimeId(),
                        InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED,
                        EVENT_AT);

        return new OutboxEventMessage(
                eventId,
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CANCELLED,
                InventoryEventContract.BOOKING_CANCELLED_VERSION,
                EVENT_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage bookingExpiredMessage(UUID eventId) {

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        context.bookingId(), context.userId(), context.showtimeId(), EVENT_AT);

        return new OutboxEventMessage(
                eventId,
                context.bookingId(),
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_EXPIRED,
                InventoryEventContract.BOOKING_EXPIRED_VERSION,
                EVENT_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void assertReleased() {

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(context.showtimeId());

        assertThat(showSeats).extracting(ShowSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(showSeats)
                .allSatisfy(
                        showSeat -> {
                            assertThat(showSeat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);

                            assertThat(showSeat.getHeldByBookingId()).isNull();

                            assertThat(showSeat.getHoldExpiresAt()).isNull();
                        });
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

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId) {}

    private record CancelledOutcome(
            BookingCancelledConsumerService.Status status, Throwable failure) {

        private static CancelledOutcome success(BookingCancelledConsumerService.Status status) {

            return new CancelledOutcome(status, null);
        }

        private static CancelledOutcome failure(Throwable failure) {

            return new CancelledOutcome(null, failure);
        }
    }

    private record ExpiredOutcome(BookingExpiredConsumerService.Status status, Throwable failure) {

        private static ExpiredOutcome success(BookingExpiredConsumerService.Status status) {

            return new ExpiredOutcome(status, null);
        }

        private static ExpiredOutcome failure(Throwable failure) {

            return new ExpiredOutcome(null, failure);
        }
    }

    private record LifecycleOutcome(boolean released, Throwable failure) {

        private static LifecycleOutcome success(boolean released) {

            return new LifecycleOutcome(released, null);
        }

        private static LifecycleOutcome failure(Throwable failure) {

            return new LifecycleOutcome(false, failure);
        }
    }
}
