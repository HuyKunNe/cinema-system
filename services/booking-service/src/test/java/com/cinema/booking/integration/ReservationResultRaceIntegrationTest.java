package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class ReservationResultRaceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    @Autowired private SeatReservedConsumerService seatReservedConsumerService;

    @Autowired
    private SeatReservationRejectedConsumerService seatReservationRejectedConsumerService;

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
    void concurrentReservedAndRejectedResultsShouldAllowOneWinner() throws Exception {

        TestContext context = createPendingBooking();

        OutboxEventMessage reservedMessage = reservedMessage(context);

        OutboxEventMessage rejectedMessage = rejectedMessage(context);

        ConcurrentResults results =
                executeConcurrently(
                        () ->
                                seatReservedConsumerService.handle(
                                        context.bookingId().toString(), reservedMessage),
                        () ->
                                seatReservationRejectedConsumerService.handle(
                                        context.bookingId().toString(), rejectedMessage));

        assertExactlyOneSuccessAndOneConflict(results);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isIn(BookingStatus.RESERVED, BookingStatus.REJECTED);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();

        if (booking.getStatus() == BookingStatus.RESERVED) {

            assertReservedState(booking, seats, context);

        } else {
            assertRejectedState(booking, seats);
        }
    }

    @Test
    void delayedReservedResultMustNotRestoreRejectedBooking() {

        TestContext context = createPendingBooking();

        OutboxEventMessage rejectedMessage = rejectedMessage(context);

        OutboxEventMessage delayedReservedMessage = reservedMessage(context);

        SeatReservationRejectedConsumerService.Result rejectionResult =
                seatReservationRejectedConsumerService.handle(
                        context.bookingId().toString(), rejectedMessage);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                seatReservedConsumerService.handle(
                                        context.bookingId().toString(), delayedReservedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(rejectionResult.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.REJECTED);

        assertRejectedState(booking, seats);

        /*
         * The rejected marker committed. The delayed reserved marker
         * rolled back with its failed aggregate transition.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void delayedRejectedResultMustNotReverseReservedBooking() {

        TestContext context = createPendingBooking();

        OutboxEventMessage reservedMessage = reservedMessage(context);

        OutboxEventMessage delayedRejectedMessage = rejectedMessage(context);

        SeatReservedConsumerService.Result reservationResult =
                seatReservedConsumerService.handle(context.bookingId().toString(), reservedMessage);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                seatReservationRejectedConsumerService.handle(
                                        context.bookingId().toString(), delayedRejectedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(reservationResult.status())
                .isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        assertReservedState(booking, seats, context);

        /*
         * The reserved marker committed. The delayed rejection marker
         * rolled back.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateReservedAfterRejectedRaceShouldRemainRejected() {

        TestContext context = createPendingBooking();

        OutboxEventMessage rejectedMessage = rejectedMessage(context);

        OutboxEventMessage reservedMessage = reservedMessage(context);

        seatReservationRejectedConsumerService.handle(
                context.bookingId().toString(), rejectedMessage);

        for (int attempt = 0; attempt < 2; attempt++) {

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () ->
                                    seatReservedConsumerService.handle(
                                            context.bookingId().toString(), reservedMessage))
                    .isInstanceOf(ConflictException.class);
        }

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertRejectedState(booking, seats);

        /*
         * Failed attempts must not leave processed-event markers.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateRejectedAfterReservedRaceShouldRemainReserved() {

        TestContext context = createPendingBooking();

        OutboxEventMessage reservedMessage = reservedMessage(context);

        OutboxEventMessage rejectedMessage = rejectedMessage(context);

        seatReservedConsumerService.handle(context.bookingId().toString(), reservedMessage);

        for (int attempt = 0; attempt < 2; attempt++) {

            org.assertj.core.api.Assertions.assertThatThrownBy(
                            () ->
                                    seatReservationRejectedConsumerService.handle(
                                            context.bookingId().toString(), rejectedMessage))
                    .isInstanceOf(ConflictException.class);
        }

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertReservedState(booking, seats, context);

        assertThat(processedEventRepository.count()).isEqualTo(1);
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

    private void assertExactlyOneSuccessAndOneConflict(ConcurrentResults results) {

        List<Object> values = List.of(results.first(), results.second());

        long successes =
                values.stream()
                        .filter(
                                value ->
                                        value instanceof SeatReservedConsumerService.Result
                                                || value
                                                        instanceof
                                                        SeatReservationRejectedConsumerService
                                                                .Result)
                        .count();

        long conflicts = values.stream().filter(ConflictException.class::isInstance).count();

        assertThat(successes).isEqualTo(1);

        assertThat(conflicts).isEqualTo(1);
    }

    private void assertReservedState(
            Booking booking, List<BookingSeat> seats, TestContext context) {

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("210000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(seats)
                .hasSize(2)
                .allSatisfy(bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isTrue());

        assertThat(seats.get(0).getSeatNumber()).isEqualTo("H7");

        assertThat(seats.get(0).getInventorySeatId()).isEqualTo(context.h7InventorySeatId());

        assertThat(seats.get(1).getSeatNumber()).isEqualTo("H8");

        assertThat(seats.get(1).getInventorySeatId()).isEqualTo(context.h8InventorySeatId());
    }

    private void assertRejectedState(Booking booking, List<BookingSeat> seats) {

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(booking.getRejectionReason()).isEqualTo("SEAT_UNAVAILABLE");

        assertThat(booking.getTotalAmount()).isNull();

        assertThat(booking.getCurrency()).isNull();

        assertThat(seats)
                .hasSize(2)
                .allSatisfy(
                        bookingSeat -> {
                            assertThat(bookingSeat.hasCompletedSnapshot()).isFalse();

                            assertThat(bookingSeat.getInventorySeatId()).isNull();

                            assertThat(bookingSeat.getSeatType()).isNull();

                            assertThat(bookingSeat.getPrice()).isNull();
                        });
    }

    private TestContext createPendingBooking() {

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        showtimeId,
                        "race-result-" + UuidGenerator.next(),
                        "a".repeat(64),
                        EXPIRES_AT,
                        NOW);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAllAndFlush(
                List.of(
                        new BookingSeat(savedBooking.getId(), showtimeId, "H7"),
                        new BookingSeat(savedBooking.getId(), showtimeId, "H8")));

        return new TestContext(
                savedBooking.getId(), showtimeId, UuidGenerator.next(), UuidGenerator.next());
    }

    private OutboxEventMessage reservedMessage(TestContext context) {

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        context.h7InventorySeatId(),
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("90000.00")),
                                new ReservedSeatPayload(
                                        context.h8InventorySeatId(),
                                        "H8",
                                        "VIP",
                                        new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                "seat-reserved",
                "1",
                NOW,
                "inventory-service",
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage rejectedMessage(TestContext context) {

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        "SEAT_UNAVAILABLE",
                        "One or more requested seats are unavailable",
                        List.of("H7"),
                        NOW);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                "seat-reservation-rejected",
                "1",
                NOW,
                "inventory-service",
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }

    private record TestContext(
            UUID bookingId, UUID showtimeId, UUID h7InventorySeatId, UUID h8InventorySeatId) {}

    private record ConcurrentResults(Object first, Object second) {}
}
