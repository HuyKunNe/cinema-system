package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.PaymentFailedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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

class PaymentFailedConsumerMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "The payment was declined";

    @Autowired private PaymentFailedConsumerService consumerService;

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
    void duplicatePaymentFailedShouldApplyOnceAndCreateOneSeatReleaseOutbox() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        PaymentFailedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), message);

        PaymentFailedConsumerService.Result duplicate =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(first.status()).isEqualTo(PaymentFailedConsumerService.Status.PAYMENT_FAILED);

        assertThat(duplicate.status()).isEqualTo(PaymentFailedConsumerService.Status.DUPLICATE);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        assertThat(booking.getRejectionReason()).isNotEqualTo(FAILURE_MESSAGE);

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        List<OutboxEventEntity> releaseEvents = findSeatReleaseEvents();

        assertThat(releaseEvents).hasSize(1);

        assertSeatReleaseRequestedOutbox(releaseEvents.getFirst(), context, message);
    }

    @Test
    void distinctPaymentFailedEventsShouldNotApplyFailureTwice() {

        TestContext context = persistReservedBooking();

        OutboxEventMessage firstMessage =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        OutboxEventMessage delayedMessage =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        PaymentFailedConsumerService.Result firstResult =
                consumerService.handle(context.bookingId().toString(), firstMessage);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), delayedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(firstResult.status())
                .isEqualTo(PaymentFailedConsumerService.Status.PAYMENT_FAILED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        /*
         * Marker của delayed event phải rollback cùng failed transition.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                firstMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedMessage.eventId(),
                                BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isFalse();

        assertThat(findSeatReleaseEvents()).hasSize(1);
    }

    @Test
    void concurrentDuplicateEventShouldApplyFailureOnce() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        List<ConcurrentOutcome> outcomes =
                executeConcurrently(context.bookingId(), message, message);

        assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome.failure()).isNull());

        assertThat(outcomes)
                .extracting(ConcurrentOutcome::status)
                .containsExactlyInAnyOrder(
                        PaymentFailedConsumerService.Status.PAYMENT_FAILED,
                        PaymentFailedConsumerService.Status.DUPLICATE);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(findSeatReleaseEvents()).hasSize(1);
    }

    @Test
    void concurrentDistinctEventsShouldAllowOnePaymentFailureWinner() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage firstMessage =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        OutboxEventMessage secondMessage =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        List<ConcurrentOutcome> outcomes =
                executeConcurrently(context.bookingId(), firstMessage, secondMessage);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() == null)
                .extracting(ConcurrentOutcome::status)
                .containsExactly(PaymentFailedConsumerService.Status.PAYMENT_FAILED);

        assertThat(outcomes)
                .filteredOn(outcome -> outcome.failure() != null)
                .singleElement()
                .satisfies(
                        outcome ->
                                assertThat(outcome.failure())
                                        .isInstanceOf(ConflictException.class));

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(booking.getRejectionReason()).isEqualTo(FAILURE_CODE);

        /*
         * Cả hai event đều có eventId khác nhau. Event thua đã
         * đăng ký marker nhưng transaction phải rollback marker đó.
         */
        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        List.of(
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        firstMessage.eventId(),
                                        BookingEventContract.PAYMENT_FAILED_CONSUMER),
                                processedEventRepository.existsByEventIdAndConsumerName(
                                        secondMessage.eventId(),
                                        BookingEventContract.PAYMENT_FAILED_CONSUMER)))
                .containsExactlyInAnyOrder(true, false);

        List<OutboxEventEntity> releaseEvents = findSeatReleaseEvents();

        assertThat(releaseEvents).hasSize(1);

        OutboxEventEntity releaseEvent = releaseEvents.getFirst();

        assertThat(releaseEvent.getCausationId())
                .isIn(firstMessage.eventId(), secondMessage.eventId());

        if (releaseEvent.getCausationId().equals(firstMessage.eventId())) {

            assertThat(releaseEvent.getCorrelationId()).isEqualTo(firstMessage.correlationId());

        } else {

            assertThat(releaseEvent.getCorrelationId()).isEqualTo(secondMessage.correlationId());
        }
    }

    private List<ConcurrentOutcome> executeConcurrently(
            UUID bookingId, OutboxEventMessage firstMessage, OutboxEventMessage secondMessage)
            throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ConcurrentOutcome> first =
                    executor.submit(
                            () -> handleConcurrently(bookingId, firstMessage, ready, start));

            Future<ConcurrentOutcome> second =
                    executor.submit(
                            () -> handleConcurrently(bookingId, secondMessage, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

        } finally {

            executor.shutdownNow();
        }
    }

    private ConcurrentOutcome handleConcurrently(
            UUID bookingId,
            OutboxEventMessage message,
            CountDownLatch ready,
            CountDownLatch start) {

        try {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
                return ConcurrentOutcome.failure(
                        new IllegalStateException(
                                "Timed out waiting to start payment-failed processing"));
            }

            PaymentFailedConsumerService.Result result =
                    consumerService.handle(bookingId.toString(), message);

            return ConcurrentOutcome.success(result.status());

        } catch (Throwable failure) {

            return ConcurrentOutcome.failure(failure);
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
                        "payment-failed-mysql-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now.minusMinutes(1));

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        UUID firstInventorySeatId = UuidGenerator.next();

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(firstInventorySeatId, "STANDARD", FIRST_SEAT_PRICE);

        UUID secondInventorySeatId = UuidGenerator.next();

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(secondInventorySeatId, "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(
                savedBooking.getId(),
                userId,
                showtimeId,
                firstInventorySeatId,
                secondInventorySeatId);
    }

    private OutboxEventMessage paymentFailedMessage(
            TestContext context, UUID eventId, UUID paymentId) {

        OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        paymentId,
                        context.bookingId(),
                        FAILURE_CODE,
                        FAILURE_MESSAGE,
                        failedAt,
                        false);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION,
                failedAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private List<OutboxEventEntity> findSeatReleaseEvents() {

        return outboxRepository.findAll().stream()
                .filter(
                        event ->
                                BookingEventContract.SEAT_RELEASE_REQUESTED.equals(
                                        event.getEventType()))
                .toList();
    }

    private void assertSeatReleaseRequestedOutbox(
            OutboxEventEntity event, TestContext context, OutboxEventMessage sourceMessage)
            throws JsonProcessingException {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.SEAT_RELEASE_REQUESTED);

        assertThat(event.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(sourceMessage.eventId());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(event.getOccurredAt());

        assertThat(event.getCreatedAt()).isEqualTo(event.getOccurredAt());

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.size()).isEqualTo(5);

        assertThat(payload.path("bookingId").asText()).isEqualTo(context.bookingId().toString());

        assertThat(payload.path("showtimeId").asText()).isEqualTo(context.showtimeId().toString());

        assertThat(payload.path("reason").asText()).isEqualTo("PAYMENT_FAILED");

        assertThat(OffsetDateTime.parse(payload.path("requestedAt").asText()))
                .isEqualTo(event.getOccurredAt());

        JsonNode seatIds = payload.path("seatIds");

        assertThat(seatIds).hasSize(2);

        assertThat(seatIds.get(0).asText()).isEqualTo(context.firstInventorySeatId().toString());

        assertThat(seatIds.get(1).asText()).isEqualTo(context.secondInventorySeatId().toString());

        assertThat(payload.has("paymentId")).isFalse();

        assertThat(payload.has("failureCode")).isFalse();

        assertThat(payload.has("message")).isFalse();

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        bookingSeatRepository.deleteAllInBatch();

        bookingRepository.deleteAllInBatch();
    }

    private record TestContext(
            UUID bookingId,
            UUID userId,
            UUID showtimeId,
            UUID firstInventorySeatId,
            UUID secondInventorySeatId) {}

    private record ConcurrentOutcome(
            PaymentFailedConsumerService.Status status, Throwable failure) {

        private static ConcurrentOutcome success(PaymentFailedConsumerService.Status status) {

            return new ConcurrentOutcome(status, null);
        }

        private static ConcurrentOutcome failure(Throwable failure) {

            return new ConcurrentOutcome(null, failure);
        }
    }
}
