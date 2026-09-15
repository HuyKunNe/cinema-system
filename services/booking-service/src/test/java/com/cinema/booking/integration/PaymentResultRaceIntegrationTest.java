package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class PaymentResultRaceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final String CURRENCY = "VND";

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "Payment was declined";

    @Autowired private PaymentSucceededConsumerService paymentSucceededConsumerService;

    @Autowired private PaymentFailedConsumerService paymentFailedConsumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void cleanBeforeTest() {

        cleanDatabase();
    }

    @AfterEach
    void cleanAfterTest() {

        cleanDatabase();
    }

    @Test
    void delayedPaymentFailureShouldNotReverseConfirmedBooking() {

        TestContext context = persistReservedBooking();

        UUID paymentId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID sourcePaymentRequestedEventId = UuidGenerator.next();

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        OutboxEventMessage succeededMessage =
                paymentSucceededMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt);

        OutboxEventMessage delayedFailedMessage =
                paymentFailedMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt.plusSeconds(1));

        PaymentSucceededConsumerService.Result succeededResult =
                paymentSucceededConsumerService.handle(
                        context.bookingId().toString(), succeededMessage);

        assertThatThrownBy(
                        () ->
                                paymentFailedConsumerService.handle(
                                        context.bookingId().toString(), delayedFailedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(succeededResult.status())
                .isEqualTo(PaymentSucceededConsumerService.Status.CONFIRMED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isNotNull();

        assertThat(booking.getRejectionReason()).isNull();

        /*
         * The failed event inserts its marker before locking the Booking.
         * Because its transition loses, that marker must roll back.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                succeededMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedFailedMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isFalse();

        assertThat(findOutboxEvents(BookingEventContract.BOOKING_CONFIRMED)).hasSize(1);

        assertThat(findOutboxEvents(BookingEventContract.SEAT_RELEASE_REQUESTED)).isEmpty();

        assertWinnerOutbox(
                findOutboxEvents(BookingEventContract.BOOKING_CONFIRMED).getFirst(),
                context.bookingId(),
                succeededMessage);
    }

    @Test
    void delayedPaymentSuccessShouldNotReversePaymentFailedBooking() {

        TestContext context = persistReservedBooking();

        UUID paymentId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID sourcePaymentRequestedEventId = UuidGenerator.next();

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        OutboxEventMessage failedMessage =
                paymentFailedMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt);

        OutboxEventMessage delayedSucceededMessage =
                paymentSucceededMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt.plusSeconds(1));

        PaymentFailedConsumerService.Result failedResult =
                paymentFailedConsumerService.handle(context.bookingId().toString(), failedMessage);

        assertThatThrownBy(
                        () ->
                                paymentSucceededConsumerService.handle(
                                        context.bookingId().toString(), delayedSucceededMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(failedResult.status())
                .isEqualTo(PaymentFailedConsumerService.Status.PAYMENT_FAILED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        /*
         * The delayed success marker must roll back with its rejected
         * Booking transition.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                failedMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedSucceededMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isFalse();

        assertThat(findOutboxEvents(BookingEventContract.SEAT_RELEASE_REQUESTED)).hasSize(1);

        assertThat(findOutboxEvents(BookingEventContract.BOOKING_CONFIRMED)).isEmpty();

        assertWinnerOutbox(
                findOutboxEvents(BookingEventContract.SEAT_RELEASE_REQUESTED).getFirst(),
                context.bookingId(),
                failedMessage);
    }

    @Test
    void concurrentSuccessAndFailureShouldAllowExactlyOneTerminalWinner() throws Exception {

        TestContext context = persistReservedBooking();

        UUID paymentId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID sourcePaymentRequestedEventId = UuidGenerator.next();

        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        OutboxEventMessage succeededMessage =
                paymentSucceededMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt);

        OutboxEventMessage failedMessage =
                paymentFailedMessage(
                        context,
                        UuidGenerator.next(),
                        paymentId,
                        correlationId,
                        sourcePaymentRequestedEventId,
                        occurredAt);

        List<ConcurrentOutcome> outcomes =
                executeConcurrently(context.bookingId(), succeededMessage, failedMessage);

        assertThat(outcomes).filteredOn(outcome -> outcome.failure() == null).hasSize(1);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() != null)
                .singleElement()
                .satisfies(
                        outcome ->
                                assertThat(outcome.failure())
                                        .isInstanceOf(ConflictException.class));

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        List<OutboxEventEntity> confirmedEvents =
                findOutboxEvents(BookingEventContract.BOOKING_CONFIRMED);

        List<OutboxEventEntity> releaseEvents =
                findOutboxEvents(BookingEventContract.SEAT_RELEASE_REQUESTED);

        assertThat(confirmedEvents.size() + releaseEvents.size()).isEqualTo(1);

        if (booking.getStatus() == BookingStatus.CONFIRMED) {

            assertConfirmedWinner(
                    context,
                    outcomes,
                    succeededMessage,
                    failedMessage,
                    confirmedEvents,
                    releaseEvents);

        } else {

            assertPaymentFailedWinner(
                    context,
                    outcomes,
                    succeededMessage,
                    failedMessage,
                    confirmedEvents,
                    releaseEvents);
        }
    }

    private void assertConfirmedWinner(
            TestContext context,
            List<ConcurrentOutcome> outcomes,
            OutboxEventMessage succeededMessage,
            OutboxEventMessage failedMessage,
            List<OutboxEventEntity> confirmedEvents,
            List<OutboxEventEntity> releaseEvents) {

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(booking.getConfirmedAt()).isNotNull();

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() == null)
                .extracting(ConcurrentOutcome::resultType)
                .containsExactly(ResultType.PAYMENT_SUCCEEDED);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                succeededMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                failedMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isFalse();

        assertThat(confirmedEvents).hasSize(1);

        assertThat(releaseEvents).isEmpty();

        assertWinnerOutbox(confirmedEvents.getFirst(), context.bookingId(), succeededMessage);
    }

    private void assertPaymentFailedWinner(
            TestContext context,
            List<ConcurrentOutcome> outcomes,
            OutboxEventMessage succeededMessage,
            OutboxEventMessage failedMessage,
            List<OutboxEventEntity> confirmedEvents,
            List<OutboxEventEntity> releaseEvents) {

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() == null)
                .extracting(ConcurrentOutcome::resultType)
                .containsExactly(ResultType.PAYMENT_FAILED);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                failedMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                succeededMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isFalse();

        assertThat(releaseEvents).hasSize(1);

        assertThat(confirmedEvents).isEmpty();

        assertWinnerOutbox(releaseEvents.getFirst(), context.bookingId(), failedMessage);
    }

    private List<ConcurrentOutcome> executeConcurrently(
            UUID bookingId, OutboxEventMessage succeededMessage, OutboxEventMessage failedMessage)
            throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ConcurrentOutcome> succeededOutcome =
                    executor.submit(
                            () ->
                                    handleSucceededConcurrently(
                                            bookingId, succeededMessage, ready, start));

            Future<ConcurrentOutcome> failedOutcome =
                    executor.submit(
                            () -> handleFailedConcurrently(bookingId, failedMessage, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            return List.of(
                    succeededOutcome.get(30, TimeUnit.SECONDS),
                    failedOutcome.get(30, TimeUnit.SECONDS));

        } finally {

            executor.shutdownNow();
        }
    }

    private ConcurrentOutcome handleSucceededConcurrently(
            UUID bookingId,
            OutboxEventMessage message,
            CountDownLatch ready,
            CountDownLatch start) {

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return ConcurrentOutcome.failure(
                        ResultType.PAYMENT_SUCCEEDED,
                        new IllegalStateException(
                                "Timed out waiting to start payment-succeeded processing"));
            }

            PaymentSucceededConsumerService.Result result =
                    paymentSucceededConsumerService.handle(bookingId.toString(), message);

            return ConcurrentOutcome.success(ResultType.PAYMENT_SUCCEEDED, result.status().name());

        } catch (Throwable failure) {

            return ConcurrentOutcome.failure(ResultType.PAYMENT_SUCCEEDED, failure);
        }
    }

    private ConcurrentOutcome handleFailedConcurrently(
            UUID bookingId,
            OutboxEventMessage message,
            CountDownLatch ready,
            CountDownLatch start) {

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {

                return ConcurrentOutcome.failure(
                        ResultType.PAYMENT_FAILED,
                        new IllegalStateException(
                                "Timed out waiting to start payment-failed processing"));
            }

            PaymentFailedConsumerService.Result result =
                    paymentFailedConsumerService.handle(bookingId.toString(), message);

            return ConcurrentOutcome.success(ResultType.PAYMENT_FAILED, result.status().name());

        } catch (Throwable failure) {

            return ConcurrentOutcome.failure(ResultType.PAYMENT_FAILED, failure);
        }
    }

    private TestContext persistReservedBooking() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-result-race-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(UuidGenerator.next(), "STANDARD", FIRST_SEAT_PRICE);

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(UuidGenerator.next(), "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(savedBooking.getId());
    }

    private OutboxEventMessage paymentSucceededMessage(
            TestContext context,
            UUID eventId,
            UUID paymentId,
            UUID correlationId,
            UUID causationId,
            OffsetDateTime occurredAt) {

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
                correlationId,
                causationId,
                objectMapper.valueToTree(payload));
    }

    private OutboxEventMessage paymentFailedMessage(
            TestContext context,
            UUID eventId,
            UUID paymentId,
            UUID correlationId,
            UUID causationId,
            OffsetDateTime occurredAt) {

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
                correlationId,
                causationId,
                objectMapper.valueToTree(payload));
    }

    private List<OutboxEventEntity> findOutboxEvents(String eventType) {

        return outboxRepository.findAll().stream()
                .filter(event -> eventType.equals(event.getEventType()))
                .toList();
    }

    private void assertWinnerOutbox(
            OutboxEventEntity event, UUID bookingId, OutboxEventMessage winningMessage) {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getCorrelationId()).isEqualTo(winningMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(winningMessage.eventId());
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        bookingSeatRepository.deleteAllInBatch();

        bookingRepository.deleteAllInBatch();
    }

    private enum ResultType {
        PAYMENT_SUCCEEDED,

        PAYMENT_FAILED
    }

    private record TestContext(UUID bookingId) {}

    private record ConcurrentOutcome(ResultType resultType, String status, Throwable failure) {

        private static ConcurrentOutcome success(ResultType resultType, String status) {

            return new ConcurrentOutcome(resultType, status, null);
        }

        private static ConcurrentOutcome failure(ResultType resultType, Throwable failure) {

            return new ConcurrentOutcome(resultType, null, failure);
        }
    }
}
