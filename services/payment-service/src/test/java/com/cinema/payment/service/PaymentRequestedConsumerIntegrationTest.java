package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

class PaymentRequestedConsumerIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private PaymentRequestedConsumerService service;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void validRequestShouldAtomicallyCreateMarkerPaymentAndCharge() {

        UUID bookingId = UuidGenerator.next();

        UUID eventId = UuidGenerator.next();

        OffsetDateTime requestedAt =
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        PaymentRequestedPayload payload =
                payload(
                        bookingId,
                        new BigDecimal("180000.00"),
                        requestedAt,
                        requestedAt.plusMinutes(10));

        OutboxEventMessage message =
                message(eventId, UuidGenerator.next(), bookingId, requestedAt, payload);

        PaymentRequestedConsumerService.Result result =
                service.handle(bookingId.toString(), message);

        assertThat(result.status()).isEqualTo(PaymentRequestedConsumerService.Status.CREATED);

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(payment.getId()).isEqualTo(result.paymentId());

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.RECEIVED);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                eventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isTrue();

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertThat(transactions).hasSize(1);

        PaymentTransaction transaction = transactions.getFirst();

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.READY);

        assertThat(transaction.getIdempotencyKey()).isEqualTo("charge:" + payment.getId());
    }

    @Test
    void duplicateSameEventShouldNotCreateAnotherPaymentOrCharge() {

        UUID bookingId = UuidGenerator.next();

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OffsetDateTime requestedAt =
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        PaymentRequestedPayload payload =
                payload(
                        bookingId,
                        new BigDecimal("180000.00"),
                        requestedAt,
                        requestedAt.plusMinutes(10));

        OutboxEventMessage message =
                message(eventId, correlationId, bookingId, requestedAt, payload);

        PaymentRequestedConsumerService.Result first =
                service.handle(bookingId.toString(), message);

        PaymentRequestedConsumerService.Result duplicate =
                service.handle(bookingId.toString(), message);

        assertThat(first.status()).isEqualTo(PaymentRequestedConsumerService.Status.CREATED);

        assertThat(duplicate.status())
                .isEqualTo(PaymentRequestedConsumerService.Status.ALREADY_PROCESSED);

        assertThat(duplicate.paymentId()).isNull();

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .hasSize(1);

        assertThat(
                        processedEventRepository.countByEventIdAndConsumerName(
                                eventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isEqualTo(1);
    }

    @Test
    void consistentDifferentEventShouldCreateOnlyAnotherMarker() {

        UUID bookingId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID firstEventId = UuidGenerator.next();

        UUID secondEventId = UuidGenerator.next();

        OffsetDateTime requestedAt =
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        PaymentRequestedPayload payload =
                payload(
                        bookingId,
                        new BigDecimal("180000.00"),
                        requestedAt,
                        requestedAt.plusMinutes(10));

        OutboxEventMessage firstMessage =
                message(firstEventId, correlationId, bookingId, requestedAt, payload);

        OutboxEventMessage secondMessage =
                message(secondEventId, correlationId, bookingId, requestedAt, payload);

        PaymentRequestedConsumerService.Result first =
                service.handle(bookingId.toString(), firstMessage);

        PaymentRequestedConsumerService.Result second =
                service.handle(bookingId.toString(), secondMessage);

        assertThat(first.status()).isEqualTo(PaymentRequestedConsumerService.Status.CREATED);

        assertThat(second.status()).isEqualTo(PaymentRequestedConsumerService.Status.EXISTING);

        assertThat(second.paymentId()).isEqualTo(first.paymentId());

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .hasSize(1);

        assertThat(
                        processedEventRepository.countByEventIdAndConsumerName(
                                firstEventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isEqualTo(1);

        assertThat(
                        processedEventRepository.countByEventIdAndConsumerName(
                                secondEventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isEqualTo(1);
    }

    @Test
    void conflictingDifferentEventShouldRollbackItsMarker() {

        UUID bookingId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID firstEventId = UuidGenerator.next();

        UUID conflictingEventId = UuidGenerator.next();

        OffsetDateTime requestedAt =
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        OffsetDateTime holdExpiresAt = requestedAt.plusMinutes(10);

        PaymentRequestedPayload initialPayload =
                payload(bookingId, new BigDecimal("180000.00"), requestedAt, holdExpiresAt);

        PaymentRequestedPayload conflictingPayload =
                payload(bookingId, new BigDecimal("190000.00"), requestedAt, holdExpiresAt);

        service.handle(
                bookingId.toString(),
                message(firstEventId, correlationId, bookingId, requestedAt, initialPayload));

        assertThatThrownBy(
                        () ->
                                service.handle(
                                        bookingId.toString(),
                                        message(
                                                conflictingEventId,
                                                correlationId,
                                                bookingId,
                                                requestedAt,
                                                conflictingPayload)))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ConflictException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PAYMENT_ATTEMPT_PAYLOAD_MISMATCH));

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                conflictingEventId,
                                PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isFalse();

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(payment.getAmount()).isEqualByComparingTo("180000.00");

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .hasSize(1);
    }

    @Test
    void expiredRequestShouldCreateExpiredPaymentWithoutCharge() {

        UUID bookingId = UuidGenerator.next();

        UUID eventId = UuidGenerator.next();

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);

        OffsetDateTime requestedAt = now.minusMinutes(10);

        OffsetDateTime holdExpiresAt = now.minusMinutes(1);

        PaymentRequestedPayload payload =
                payload(bookingId, new BigDecimal("180000.00"), requestedAt, holdExpiresAt);

        PaymentRequestedConsumerService.Result result =
                service.handle(
                        bookingId.toString(),
                        message(eventId, UuidGenerator.next(), bookingId, requestedAt, payload));

        assertThat(result.status()).isEqualTo(PaymentRequestedConsumerService.Status.EXPIRED);

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);

        assertThat(payment.getFailureCode()).isEqualTo("RESERVATION_EXPIRED");

        assertThat(payment.getCompletedAt()).isNotNull();

        assertThat(transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId()))
                .isEmpty();

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                eventId, PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isTrue();
    }

    private PaymentRequestedPayload payload(
            UUID bookingId,
            BigDecimal amount,
            OffsetDateTime requestedAt,
            OffsetDateTime holdExpiresAt) {

        return new PaymentRequestedPayload(
                bookingId, UuidGenerator.next(), amount, "VND", 1, holdExpiresAt, requestedAt);
    }

    private OutboxEventMessage message(
            UUID eventId,
            UUID correlationId,
            UUID bookingId,
            OffsetDateTime occurredAt,
            PaymentRequestedPayload payload) {

        JsonNode payloadJson = objectMapper.valueToTree(payload);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                "BOOKING",
                PaymentEventContract.PAYMENT_REQUESTED,
                PaymentEventContract.PAYMENT_REQUESTED_VERSION,
                occurredAt,
                "booking-service",
                correlationId,
                UuidGenerator.next(),
                payloadJson);
    }
}
