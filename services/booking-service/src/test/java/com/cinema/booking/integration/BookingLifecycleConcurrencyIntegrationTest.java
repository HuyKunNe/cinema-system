package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.booking.service.BookingExpirationService;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class BookingLifecycleConcurrencyIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private BookingCancellationService cancellationService;

    @Autowired private BookingExpirationService expirationService;

    @Autowired private SeatReservedConsumerService seatReservedConsumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void cleanDatabaseBeforeTest() {

        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        cleanDatabase();
    }

    @Test
    void concurrentCancellationShouldCreateOneTransitionAndOneOutbox() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        ConcurrentResults results =
                executeConcurrently(
                        () -> cancellationService.cancel(context.userId(), context.bookingId()),
                        () -> cancellationService.cancel(context.userId(), context.bookingId()));

        List<Object> values = results.values();

        assertThat(values).filteredOn(BookingResponse.class::isInstance).hasSize(1);

        assertThat(values).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isNotNull();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).isEmpty();

        assertThat(lifecycleEvents(BookingEventContract.PAYMENT_REQUESTED)).isEmpty();
    }

    @Test
    void cancellationShouldWinAgainstDelayedSeatReserved() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        OutboxEventMessage delayedSeatReserved = seatReservedMessage(context, now.minusSeconds(30));

        ConcurrentResults results =
                executeConcurrently(
                        () -> cancellationService.cancel(context.userId(), context.bookingId()),
                        () ->
                                seatReservedConsumerService.handle(
                                        context.bookingId().toString(), delayedSeatReserved));

        List<Object> values = results.values();

        assertThat(values).filteredOn(BookingResponse.class::isInstance).hasSize(1);

        assertThat(values).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isNotNull();

        /*
         * The delayed reservation transaction may temporarily complete
         * the seat snapshot before the aggregate rejects RESERVED ->
         * RESERVED. Its transaction must roll back that snapshot.
         */
        assertThat(bookingSeats)
                .singleElement()
                .satisfies(
                        bookingSeat -> {
                            assertThat(bookingSeat.hasCompletedSnapshot()).isFalse();

                            assertThat(bookingSeat.getInventorySeatId()).isNull();

                            assertThat(bookingSeat.getPrice()).isNull();
                        });

        /*
         * The delayed reservation marker rolls back with the failed
         * aggregate transition.
         */
        assertThat(processedEventRepository.count()).isZero();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

        assertThat(lifecycleEvents(BookingEventContract.PAYMENT_REQUESTED)).isEmpty();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).isEmpty();
    }

    @Test
    void expirationShouldWinAgainstDelayedSeatReserved() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(10), now.minusSeconds(1));

        OutboxEventMessage delayedSeatReserved = seatReservedMessage(context, now.minusMinutes(5));

        ConcurrentResults results =
                executeConcurrently(
                        () -> expirationService.expireIfDue(context.bookingId()),
                        () ->
                                seatReservedConsumerService.handle(
                                        context.bookingId().toString(), delayedSeatReserved));

        List<Object> values = results.values();

        assertThat(values).filteredOn(value -> Boolean.TRUE.equals(value)).hasSize(1);

        assertThat(values).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(bookingSeats)
                .singleElement()
                .satisfies(
                        bookingSeat -> {
                            assertThat(bookingSeat.hasCompletedSnapshot()).isFalse();

                            assertThat(bookingSeat.getInventorySeatId()).isNull();

                            assertThat(bookingSeat.getPrice()).isNull();
                        });

        assertThat(processedEventRepository.count()).isZero();

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(lifecycleEvents(BookingEventContract.BOOKING_CANCELLED)).isEmpty();

        assertThat(lifecycleEvents(BookingEventContract.PAYMENT_REQUESTED)).isEmpty();
    }

    private TestContext createReservedBooking(OffsetDateTime createdAt, OffsetDateTime expiresAt) {

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID inventorySeatId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "lifecycle-race-" + UuidGenerator.next(),
                        "a".repeat(64),
                        expiresAt,
                        createdAt);

        booking.reserve(new BigDecimal("180000.00"), "VND");

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(savedBooking.getId(), showtimeId, "H7"));

        return new TestContext(
                savedBooking.getId(), userId, showtimeId, inventorySeatId, createdAt, expiresAt);
    }

    private OutboxEventMessage seatReservedMessage(TestContext context, OffsetDateTime occurredAt) {

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        context.inventorySeatId(),
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("180000.00"))),
                        new BigDecimal("180000.00"),
                        "VND",
                        occurredAt,
                        context.expiresAt());

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                BookingEventContract.SEAT_RESERVED,
                BookingEventContract.SEAT_RESERVED_VERSION,
                occurredAt,
                BookingEventContract.INVENTORY_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
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

            start.await();

            try {
                return task.call();

            } catch (RuntimeException exception) {
                return exception;
            }
        };
    }

    private List<OutboxEventEntity> lifecycleEvents(String eventType) {

        return outboxRepository.findAll().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .toList();
    }

    private OffsetDateTime currentTime() {

        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }

    private record TestContext(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            UUID inventorySeatId,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt) {}

    private record ConcurrentResults(Object first, Object second) {

        List<Object> values() {

            return List.of(first, second);
        }
    }
}
