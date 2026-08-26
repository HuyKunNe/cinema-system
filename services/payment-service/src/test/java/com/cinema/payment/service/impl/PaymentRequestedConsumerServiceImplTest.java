package com.cinema.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.config.PaymentProperties;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.event.serialization.PaymentRequestedPayloadReader;
import com.cinema.payment.event.validation.PaymentRequestedMessageValidator;
import com.cinema.payment.event.validation.PaymentRequestedPayloadValidator;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.service.PaymentRequestedConsumerService;
import com.cinema.payment.service.ProcessedEventRegistrationService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentRequestedConsumerServiceImplTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final OffsetDateTime REQUESTED_AT =
            NOW.minusMinutes(1);

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            NOW.plusMinutes(9);

    private static final UUID BOOKING_ID =
            UuidGenerator.next();

    private static final UUID USER_ID =
            UuidGenerator.next();

    private static final UUID EVENT_ID =
            UuidGenerator.next();

    private static final UUID CORRELATION_ID =
            UuidGenerator.next();

    private static final UUID CAUSATION_ID =
            UuidGenerator.next();

    @Mock
    private PaymentRequestedMessageValidator messageValidator;

    @Mock
    private PaymentRequestedPayloadReader payloadReader;

    @Mock
    private PaymentRequestedPayloadValidator payloadValidator;

    @Mock
    private ProcessedEventRegistrationService
            processedEventRegistrationService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    private PaymentRequestedConsumerServiceImpl service;

    @BeforeEach
    void setUp() {

        Clock clock =
                Clock.fixed(
                        NOW.toInstant(),
                        ZoneOffset.UTC);

        PaymentProperties properties =
                new PaymentProperties("MOCK");

        service =
                new PaymentRequestedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        paymentRepository,
                        transactionRepository,
                        properties,
                        clock);
    }

    @Test
    void newRequestShouldCreatePaymentAndReadyCharge() {

        PaymentRequestedPayload payload =
                validPayload();

        OutboxEventMessage message =
                message(
                        EVENT_ID,
                        CORRELATION_ID,
                        BOOKING_ID);

        when(payloadReader.read(message))
                .thenReturn(payload);

        when(processedEventRegistrationService.register(
                        EVENT_ID,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_CONSUMER,
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION))
                .thenReturn(true);

        when(paymentRepository
                        .findByBookingIdAndPaymentAttempt(
                                BOOKING_ID,
                                1))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0));

        PaymentRequestedConsumerService.Result result =
                service.handle(
                        BOOKING_ID.toString(),
                        message);

        assertThat(result.status())
                .isEqualTo(
                        PaymentRequestedConsumerService.Status.CREATED);

        assertThat(result.paymentId()).isNotNull();

        verify(messageValidator)
                .validate(
                        BOOKING_ID.toString(),
                        message);

        verify(payloadReader).read(message);

        verify(payloadValidator)
                .validate(message, payload);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(paymentCaptor.capture());

        Payment savedPayment =
                paymentCaptor.getValue();

        assertThat(savedPayment.getId())
                .isEqualTo(result.paymentId());

        assertThat(savedPayment.getBookingId())
                .isEqualTo(BOOKING_ID);

        assertThat(savedPayment.getUserId())
                .isEqualTo(USER_ID);

        assertThat(savedPayment.getAmount())
                .isEqualByComparingTo("180000.00");

        assertThat(savedPayment.getCurrency())
                .isEqualTo("VND");

        assertThat(savedPayment.getProvider())
                .isEqualTo("MOCK");

        assertThat(savedPayment.getStatus())
                .isEqualTo(PaymentStatus.RECEIVED);

        ArgumentCaptor<PaymentTransaction> transactionCaptor =
                ArgumentCaptor.forClass(
                        PaymentTransaction.class);

        verify(transactionRepository)
                .save(transactionCaptor.capture());

        PaymentTransaction transaction =
                transactionCaptor.getValue();

        assertThat(transaction.getPaymentId())
                .isEqualTo(savedPayment.getId());

        assertThat(transaction.getProvider())
                .isEqualTo("MOCK");

        assertThat(transaction.getStatus())
                .isEqualTo(
                        PaymentTransactionStatus.READY);

        assertThat(transaction.getAmount())
                .isEqualByComparingTo("180000.00");

        assertThat(transaction.getCurrency())
                .isEqualTo("VND");

        assertThat(transaction.getIdempotencyKey())
                .isEqualTo(
                        "charge:" + savedPayment.getId());

        assertThat(transaction.getRequestedAt())
                .isEqualTo(NOW);
    }

    @Test
    void duplicateSameEventShouldReturnAlreadyProcessed() {

        PaymentRequestedPayload payload =
                validPayload();

        OutboxEventMessage message =
                message(
                        EVENT_ID,
                        CORRELATION_ID,
                        BOOKING_ID);

        when(payloadReader.read(message))
                .thenReturn(payload);

        when(processedEventRegistrationService.register(
                        EVENT_ID,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_CONSUMER,
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION))
                .thenReturn(false);

        PaymentRequestedConsumerService.Result result =
                service.handle(
                        BOOKING_ID.toString(),
                        message);

        assertThat(result.status())
                .isEqualTo(
                        PaymentRequestedConsumerService.Status
                                .ALREADY_PROCESSED);

        assertThat(result.paymentId()).isNull();

        verify(messageValidator)
                .validate(
                        BOOKING_ID.toString(),
                        message);

        verify(payloadReader).read(message);

        verify(payloadValidator)
                .validate(message, payload);

        verifyNoInteractions(
                paymentRepository,
                transactionRepository);
    }

    @Test
    void consistentDifferentEventShouldReturnExistingPayment() {

        UUID existingSourceEventId =
                UuidGenerator.next();

        UUID duplicateEventId =
                UuidGenerator.next();

        Payment existing =
                existingPayment(
                        existingSourceEventId,
                        CORRELATION_ID);

        PaymentRequestedPayload payload =
                validPayload();

        OutboxEventMessage message =
                message(
                        duplicateEventId,
                        CORRELATION_ID,
                        BOOKING_ID);

        when(payloadReader.read(message))
                .thenReturn(payload);

        when(processedEventRegistrationService.register(
                        duplicateEventId,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_CONSUMER,
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION))
                .thenReturn(true);

        when(paymentRepository
                        .findByBookingIdAndPaymentAttempt(
                                BOOKING_ID,
                                1))
                .thenReturn(Optional.of(existing));

        PaymentRequestedConsumerService.Result result =
                service.handle(
                        BOOKING_ID.toString(),
                        message);

        assertThat(result.status())
                .isEqualTo(
                        PaymentRequestedConsumerService.Status.EXISTING);

        assertThat(result.paymentId())
                .isEqualTo(existing.getId());

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void conflictingAmountShouldBeRejected() {

        UUID duplicateEventId =
                UuidGenerator.next();

        Payment existing =
                existingPayment(
                        UuidGenerator.next(),
                        CORRELATION_ID);

        PaymentRequestedPayload conflictingPayload =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("190000.00"),
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        OutboxEventMessage message =
                message(
                        duplicateEventId,
                        CORRELATION_ID,
                        BOOKING_ID);

        when(payloadReader.read(message))
                .thenReturn(conflictingPayload);

        when(processedEventRegistrationService.register(
                        duplicateEventId,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_CONSUMER,
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION))
                .thenReturn(true);

        when(paymentRepository
                        .findByBookingIdAndPaymentAttempt(
                                BOOKING_ID,
                                1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                service.handle(
                                        BOOKING_ID.toString(),
                                        message))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ConflictException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_ATTEMPT_PAYLOAD_MISMATCH));

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void conflictingCorrelationIdShouldBeRejected() {

        UUID duplicateEventId =
                UuidGenerator.next();

        Payment existing =
                existingPayment(
                        UuidGenerator.next(),
                        CORRELATION_ID);

        OutboxEventMessage message =
                message(
                        duplicateEventId,
                        UuidGenerator.next(),
                        BOOKING_ID);

        PaymentRequestedPayload payload =
                validPayload();

        when(payloadReader.read(message))
                .thenReturn(payload);

        when(processedEventRegistrationService.register(
                        eq(duplicateEventId),
                        eq(
                                PaymentEventContract
                                        .PAYMENT_REQUESTED_CONSUMER),
                        eq(
                                PaymentEventContract
                                        .PAYMENT_REQUESTED),
                        eq(
                                PaymentEventContract
                                        .PAYMENT_REQUESTED_VERSION)))
                .thenReturn(true);

        when(paymentRepository
                        .findByBookingIdAndPaymentAttempt(
                                BOOKING_ID,
                                1))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                service.handle(
                                        BOOKING_ID.toString(),
                                        message))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((ConflictException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PAYMENT_ATTEMPT_PAYLOAD_MISMATCH));

        verify(paymentRepository, never())
                .save(any(Payment.class));

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void expiredRequestShouldCreateExpiredPaymentWithoutCharge() {

        OffsetDateTime expiredRequestedAt =
                NOW.minusMinutes(10);

        OffsetDateTime expiredHoldAt =
                NOW.minusMinutes(1);

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        expiredHoldAt,
                        expiredRequestedAt);

        OutboxEventMessage message =
                message(
                        EVENT_ID,
                        CORRELATION_ID,
                        BOOKING_ID);

        when(payloadReader.read(message))
                .thenReturn(payload);

        when(processedEventRegistrationService.register(
                        EVENT_ID,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_CONSUMER,
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract
                                .PAYMENT_REQUESTED_VERSION))
                .thenReturn(true);

        when(paymentRepository
                        .findByBookingIdAndPaymentAttempt(
                                BOOKING_ID,
                                1))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0));

        PaymentRequestedConsumerService.Result result =
                service.handle(
                        BOOKING_ID.toString(),
                        message);

        assertThat(result.status())
                .isEqualTo(
                        PaymentRequestedConsumerService.Status.EXPIRED);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository)
                .save(paymentCaptor.capture());

        Payment expiredPayment =
                paymentCaptor.getValue();

        assertThat(expiredPayment.getStatus())
                .isEqualTo(PaymentStatus.EXPIRED);

        assertThat(expiredPayment.getFailureCode())
                .isEqualTo("RESERVATION_EXPIRED");

        assertThat(expiredPayment.getCompletedAt())
                .isEqualTo(NOW);

        verifyNoInteractions(transactionRepository);
    }

    private static PaymentRequestedPayload validPayload() {

        return new PaymentRequestedPayload(
                BOOKING_ID,
                USER_ID,
                new BigDecimal("180000.00"),
                "VND",
                1,
                HOLD_EXPIRES_AT,
                REQUESTED_AT);
    }

    private static Payment existingPayment(
            UUID sourceEventId,
            UUID correlationId) {

        return new Payment(
                BOOKING_ID,
                USER_ID,
                1,
                new BigDecimal("180000.00"),
                "VND",
                "MOCK",
                HOLD_EXPIRES_AT,
                REQUESTED_AT,
                sourceEventId,
                correlationId);
    }

    private static OutboxEventMessage message(
            UUID eventId,
            UUID correlationId,
            UUID bookingId) {

        return new OutboxEventMessage(
                eventId,
                bookingId,
                "BOOKING",
                PaymentEventContract.PAYMENT_REQUESTED,
                PaymentEventContract
                        .PAYMENT_REQUESTED_VERSION,
                REQUESTED_AT,
                "booking-service",
                correlationId,
                CAUSATION_ID,
                JsonNodeFactory.instance.objectNode());
    }
}
