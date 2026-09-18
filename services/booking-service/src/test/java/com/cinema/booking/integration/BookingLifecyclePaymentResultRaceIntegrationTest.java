package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.booking.service.BookingExpirationService;
import com.cinema.booking.service.PaymentFailedConsumerService;
import com.cinema.booking.service.PaymentSucceededConsumerService;
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

class BookingLifecyclePaymentResultRaceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "Payment was declined";

    @Autowired private BookingCancellationService cancellationService;

    @Autowired private BookingExpirationService expirationService;

    @Autowired private PaymentSucceededConsumerService paymentSucceededConsumerService;

    @Autowired private PaymentFailedConsumerService paymentFailedConsumerService;

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
    void cancelledBookingShouldRejectDelayedPaymentSuccess() {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        OutboxEventMessage paymentSucceeded = paymentSucceededMessage(context, now);

        BookingResponse cancelled =
                cancellationService.cancel(context.userId(), context.bookingId());

        assertThat(cancelled.status()).isEqualTo(BookingStatus.CANCELLED);

        assertThatThrownBy(
                        () ->
                                paymentSucceededConsumerService.handle(
                                        context.bookingId().toString(), paymentSucceeded))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isNotNull();

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(events(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void cancelledBookingShouldRejectDelayedPaymentFailure() {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        OutboxEventMessage paymentFailed = paymentFailedMessage(context, now);

        cancellationService.cancel(context.userId(), context.bookingId());

        assertThatThrownBy(
                        () ->
                                paymentFailedConsumerService.handle(
                                        context.bookingId().toString(), paymentFailed))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isNotNull();

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(events(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void expiredBookingShouldRejectDelayedPaymentSuccess() {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(10), now.minusSeconds(1));

        OutboxEventMessage paymentSucceeded = paymentSucceededMessage(context, now);

        boolean expired = expirationService.expireIfDue(context.bookingId());

        assertThat(expired).isTrue();

        assertThatThrownBy(
                        () ->
                                paymentSucceededConsumerService.handle(
                                        context.bookingId().toString(), paymentSucceeded))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(events(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void expiredBookingShouldRejectDelayedPaymentFailure() {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(10), now.minusSeconds(1));

        OutboxEventMessage paymentFailed = paymentFailedMessage(context, now);

        boolean expired = expirationService.expireIfDue(context.bookingId());

        assertThat(expired).isTrue();

        assertThatThrownBy(
                        () ->
                                paymentFailedConsumerService.handle(
                                        context.bookingId().toString(), paymentFailed))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(events(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void concurrentCancellationAndPaymentSuccessShouldHaveOneTerminalWinner() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        OutboxEventMessage paymentSucceeded = paymentSucceededMessage(context, now);

        ConcurrentResults results =
                executeConcurrently(
                        () -> cancellationService.cancel(context.userId(), context.bookingId()),
                        () ->
                                paymentSucceededConsumerService.handle(
                                        context.bookingId().toString(), paymentSucceeded));

        assertThat(results.values()).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            assertThat(results.values()).filteredOn(BookingResponse.class::isInstance).hasSize(1);

            assertThat(booking.getCancelledAt()).isNotNull();

            assertThat(booking.getConfirmedAt()).isNull();

            assertThat(processedEventRepository.count()).isZero();

            assertThat(events(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

            assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        } else {

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            assertThat(results.values())
                    .filteredOn(PaymentSucceededConsumerService.Result.class::isInstance)
                    .hasSize(1);

            assertThat(booking.getConfirmedAt()).isNotNull();

            assertThat(booking.getCancelledAt()).isNull();

            assertThat(processedEventRepository.count()).isEqualTo(1);

            assertThat(events(BookingEventContract.BOOKING_CONFIRMED))
                    .singleElement()
                    .satisfies(
                            event ->
                                    assertPaymentResultOutbox(
                                            event, context.bookingId(), paymentSucceeded));

            assertThat(events(BookingEventContract.BOOKING_CANCELLED)).isEmpty();
        }

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void concurrentCancellationAndPaymentFailureShouldHaveOneTerminalWinner() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(1), now.plusMinutes(10));

        OutboxEventMessage paymentFailed = paymentFailedMessage(context, now);

        ConcurrentResults results =
                executeConcurrently(
                        () -> cancellationService.cancel(context.userId(), context.bookingId()),
                        () ->
                                paymentFailedConsumerService.handle(
                                        context.bookingId().toString(), paymentFailed));

        assertThat(results.values()).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        if (booking.getStatus() == BookingStatus.CANCELLED) {

            assertThat(results.values()).filteredOn(BookingResponse.class::isInstance).hasSize(1);

            assertThat(booking.getCancelledAt()).isNotNull();

            assertThat(booking.getRejectionReason()).isNull();

            assertThat(processedEventRepository.count()).isZero();

            assertThat(events(BookingEventContract.BOOKING_CANCELLED)).hasSize(1);

            assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();

        } else {

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

            assertThat(results.values())
                    .filteredOn(PaymentFailedConsumerService.Result.class::isInstance)
                    .hasSize(1);

            assertThat(booking.getCancelledAt()).isNull();

            assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

            assertThat(processedEventRepository.count()).isEqualTo(1);

            assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED))
                    .singleElement()
                    .satisfies(
                            event ->
                                    assertPaymentResultOutbox(
                                            event, context.bookingId(), paymentFailed));

            assertThat(events(BookingEventContract.BOOKING_CANCELLED)).isEmpty();
        }

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();
    }

    @Test
    void concurrentExpirationAndPaymentSuccessShouldExpireBooking() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(10), now.minusSeconds(1));

        OutboxEventMessage paymentSucceeded = paymentSucceededMessage(context, now);

        ConcurrentResults results =
                executeConcurrently(
                        () -> expirationService.expireIfDue(context.bookingId()),
                        () ->
                                paymentSucceededConsumerService.handle(
                                        context.bookingId().toString(), paymentSucceeded));

        assertThat(results.values()).filteredOn(value -> Boolean.TRUE.equals(value)).hasSize(1);

        assertThat(results.values()).filteredOn(ConflictException.class::isInstance).hasSize(1);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(events(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();
    }

    @Test
    void concurrentExpirationAndPaymentFailureShouldConvergeToOneTerminalState() throws Exception {

        OffsetDateTime now = currentTime();

        TestContext context = createReservedBooking(now.minusMinutes(10), now.minusSeconds(1));

        OutboxEventMessage paymentFailed = paymentFailedMessage(context, now);

        ConcurrentResults results =
                executeConcurrently(
                        () -> expirationService.expireIfDue(context.bookingId()),
                        () ->
                                paymentFailedConsumerService.handle(
                                        context.bookingId().toString(), paymentFailed));

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        if (booking.getStatus() == BookingStatus.EXPIRED) {

            assertThat(results.values()).filteredOn(value -> Boolean.TRUE.equals(value)).hasSize(1);

            assertThat(results.values()).filteredOn(ConflictException.class::isInstance).hasSize(1);

            assertThat(booking.getRejectionReason()).isNull();

            assertThat(processedEventRepository.count()).isZero();

            assertThat(events(BookingEventContract.BOOKING_EXPIRED)).hasSize(1);

            assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();

        } else {

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

            assertThat(results.values())
                    .filteredOn(value -> Boolean.FALSE.equals(value))
                    .hasSize(1);

            assertThat(results.values())
                    .filteredOn(PaymentFailedConsumerService.Result.class::isInstance)
                    .hasSize(1);

            assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

            assertThat(processedEventRepository.count()).isEqualTo(1);

            assertThat(events(BookingEventContract.SEAT_RELEASE_REQUESTED))
                    .singleElement()
                    .satisfies(
                            event ->
                                    assertPaymentResultOutbox(
                                            event, context.bookingId(), paymentFailed));

            assertThat(events(BookingEventContract.BOOKING_EXPIRED)).isEmpty();
        }

        assertThat(events(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();
    }

    private TestContext createReservedBooking(OffsetDateTime createdAt, OffsetDateTime expiresAt) {

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "lifecycle-payment-race-" + UuidGenerator.next(),
                        "a".repeat(64),
                        expiresAt,
                        createdAt);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(UuidGenerator.next(), "STANDARD", FIRST_SEAT_PRICE);

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(UuidGenerator.next(), "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(savedBooking.getId(), userId, showtimeId);
    }

    private OutboxEventMessage paymentSucceededMessage(
            TestContext context, OffsetDateTime occurredAt) {

        UUID eventId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        paymentId,
                        context.bookingId(),
                        TOTAL_AMOUNT,
                        CURRENCY,
                        "MOMO",
                        "momo-" + paymentId,
                        occurredAt);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                occurredAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage paymentFailedMessage(
            TestContext context, OffsetDateTime occurredAt) {

        UUID eventId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        paymentId,
                        context.bookingId(),
                        FAILURE_CODE,
                        FAILURE_MESSAGE,
                        occurredAt,
                        false);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION,
                occurredAt,
                BookingEventContract.PAYMENT_PRODUCER,
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

    private List<OutboxEventEntity> events(String eventType) {

        return outboxRepository.findAll().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .toList();
    }

    private void assertPaymentResultOutbox(
            OutboxEventEntity event, UUID bookingId, OutboxEventMessage sourceMessage) {

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(sourceMessage.eventId());
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

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId) {}

    private record ConcurrentResults(Object first, Object second) {

        List<Object> values() {

            return List.of(first, second);
        }
    }
}
