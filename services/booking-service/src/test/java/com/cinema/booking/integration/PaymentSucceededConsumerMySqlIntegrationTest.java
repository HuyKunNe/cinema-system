package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
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

class PaymentSucceededConsumerMySqlIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    @Autowired private PaymentSucceededConsumerService consumerService;

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
    void duplicatePaymentSucceededShouldConfirmOnceAndCreateOneOutbox() {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentSucceededMessage(context, UuidGenerator.next(), UuidGenerator.next());

        PaymentSucceededConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), message);

        PaymentSucceededConsumerService.Result duplicate =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(first.status()).isEqualTo(PaymentSucceededConsumerService.Status.CONFIRMED);

        assertThat(duplicate.status()).isEqualTo(PaymentSucceededConsumerService.Status.DUPLICATE);

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(reloaded.getConfirmedAt()).isNotNull();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        List<OutboxEventEntity> confirmedEvents =
                outboxRepository.findAll().stream()
                        .filter(
                                event ->
                                        BookingEventContract.BOOKING_CONFIRMED.equals(
                                                event.getEventType()))
                        .toList();

        assertThat(confirmedEvents).hasSize(1);

        assertBookingConfirmedOutbox(confirmedEvents.getFirst(), context, message);
    }

    @Test
    void distinctPaymentEventsShouldNotConfirmBookingTwice() {

        TestContext context = persistReservedBooking();

        OutboxEventMessage firstMessage =
                paymentSucceededMessage(context, UuidGenerator.next(), UuidGenerator.next());

        OutboxEventMessage delayedMessage =
                paymentSucceededMessage(context, UuidGenerator.next(), UuidGenerator.next());

        PaymentSucceededConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), firstMessage);

        assertThatThrownBy(
                        () ->
                                consumerService.handle(
                                        context.bookingId().toString(), delayedMessage))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(result.status()).isEqualTo(PaymentSucceededConsumerService.Status.CONFIRMED);

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                firstMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isTrue();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                delayedMessage.eventId(),
                                BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isFalse();

        assertThat(outboxRepository.findAll())
                .filteredOn(
                        event ->
                                BookingEventContract.BOOKING_CONFIRMED.equals(event.getEventType()))
                .hasSize(1);
    }

    @Test
    void concurrentDuplicateEventShouldConfirmOnce() throws Exception {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentSucceededMessage(context, UuidGenerator.next(), UuidGenerator.next());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<PaymentSucceededConsumerService.Result> first =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return consumerService.handle(
                                        context.bookingId().toString(), message);
                            });

            Future<PaymentSucceededConsumerService.Result> second =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                start.await();

                                return consumerService.handle(
                                        context.bookingId().toString(), message);
                            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            List<PaymentSucceededConsumerService.Status> statuses =
                    List.of(
                            first.get(30, TimeUnit.SECONDS).status(),
                            second.get(30, TimeUnit.SECONDS).status());

            assertThat(statuses)
                    .containsExactlyInAnyOrder(
                            PaymentSucceededConsumerService.Status.CONFIRMED,
                            PaymentSucceededConsumerService.Status.DUPLICATE);

            entityManager.clear();

            Booking reloaded = bookingRepository.findById(context.bookingId()).orElseThrow();

            assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(processedEventRepository.count()).isEqualTo(1);

            assertThat(outboxRepository.findAll())
                    .filteredOn(
                            event ->
                                    BookingEventContract.BOOKING_CONFIRMED.equals(
                                            event.getEventType()))
                    .hasSize(1);
        } finally {
            executor.shutdownNow();
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
                        "payment-success-mysql-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSeat h7 = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        h7.completeSnapshot(UuidGenerator.next(), "STANDARD", new BigDecimal("90000.00"));

        BookingSeat h8 = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        h8.completeSnapshot(UuidGenerator.next(), "VIP", new BigDecimal("120000.00"));

        List<BookingSeat> seats = bookingSeatRepository.saveAllAndFlush(List.of(h7, h8));

        return new TestContext(
                savedBooking.getId(), userId, showtimeId, savedBooking.getExpiresAt(), seats);
    }

    private OutboxEventMessage paymentSucceededMessage(
            TestContext context, UUID eventId, UUID paymentId) {

        OffsetDateTime paidAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        paymentId,
                        context.bookingId(),
                        TOTAL_AMOUNT,
                        CURRENCY,
                        "MOMO",
                        "momo-" + paymentId,
                        paidAt);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                paidAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void assertBookingConfirmedOutbox(
            OutboxEventEntity event, TestContext context, OutboxEventMessage sourceMessage) {

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");
        assertThat(event.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.BOOKING_CONFIRMED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.BOOKING_CONFIRMED);

        assertThat(event.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(sourceMessage.correlationId());

        assertThat(event.getCausationId()).isEqualTo(sourceMessage.eventId());

        assertThat(event.getId()).isNotNull();
        assertThat(event.getId().version()).isEqualTo(7);
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
            OffsetDateTime expiresAt,
            List<BookingSeat> seats) {}
}
