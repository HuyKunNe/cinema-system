package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.repository.FinancialAuditRecordRepository;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Import(PaymentTracePropagationIntegrationTest.FixedClockConfiguration.class)
@TestPropertySource(
        properties = {
            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.outbox.enabled=false"
        })
class PaymentTracePropagationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant CURRENT_INSTANT = Instant.parse("2026-09-18T10:00:00Z");

    private static final OffsetDateTime NOW =
            OffsetDateTime.ofInstant(CURRENT_INSTANT, ZoneOffset.UTC);

    private static final BigDecimal AMOUNT = new BigDecimal("180000.00");

    private static final String CURRENCY = "VND";

    private static final String PROCESSING_OWNER = "payment-trace-propagation-worker";

    @Autowired private PaymentRequestedConsumerService requestedConsumerService;

    @Autowired private PaymentProviderResultApplicationService resultApplicationService;

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private PaymentProviderWebhookEventRepository webhookEventRepository;

    @Autowired private FinancialAuditRecordRepository auditRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        webhookEventRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        auditRepository.deleteAllInBatch();

        transactionRepository.deleteAllInBatch();

        paymentRepository.deleteAllInBatch();
    }

    @Test
    void paymentRequestedTraceShouldPropagateToPaymentSucceeded() {

        UUID bookingId = UuidGenerator.next();

        UUID paymentRequestedEventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage paymentRequested =
                paymentRequestedMessage(bookingId, paymentRequestedEventId, correlationId);

        PaymentRequestedConsumerService.Result requestedResult =
                requestedConsumerService.handle(bookingId.toString(), paymentRequested);

        assertThat(requestedResult.status())
                .isEqualTo(PaymentRequestedConsumerService.Status.CREATED);

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        /*
         * First cross-service handoff:
         *
         * Booking payment-requested
         *        ↓
         * Payment aggregate
         */
        assertThat(payment.getSourceEventId()).isEqualTo(paymentRequestedEventId);

        assertThat(payment.getCorrelationId()).isEqualTo(correlationId);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                paymentRequestedEventId,
                                PaymentEventContract.PAYMENT_REQUESTED_CONSUMER))
                .isTrue();

        ClaimedProviderChargeOperation operation = claimChargeOperation(payment);

        ProviderChargeResult providerResult =
                ProviderChargeResult.succeeded("trace-provider-reference-success");

        resultApplicationService.apply(operation, providerResult);

        entityManager.clear();

        Payment completedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        /*
         * The Payment aggregate must keep the tracing identity
         * originating from payment-requested.
         */
        assertThat(completedPayment.getSourceEventId()).isEqualTo(paymentRequestedEventId);

        assertThat(completedPayment.getCorrelationId()).isEqualTo(correlationId);

        OutboxEventEntity terminalEvent = onlyOutboxEvent();

        assertThat(terminalEvent.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_SUCCEEDED);

        /*
         * Cross-service trace propagation:
         *
         * payment-requested.correlationId
         *        ==
         * Payment.correlationId
         *        ==
         * payment-succeeded.correlationId
         */
        assertThat(terminalEvent.getCorrelationId()).isEqualTo(correlationId);

        /*
         * Payment has no intermediate Kafka event between
         * payment-requested and the provider terminal result.
         *
         * Therefore payment-requested remains the direct Saga
         * event cause of payment-succeeded.
         */
        assertThat(terminalEvent.getCausationId()).isEqualTo(paymentRequestedEventId);

        assertThat(terminalEvent.getAggregateId()).isEqualTo(completedPayment.getId());

        assertThat(terminalEvent.getPartitionKey()).isEqualTo(bookingId.toString());
    }

    @Test
    void paymentRequestedTraceShouldPropagateToPaymentFailed() {

        UUID bookingId = UuidGenerator.next();

        UUID paymentRequestedEventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage paymentRequested =
                paymentRequestedMessage(bookingId, paymentRequestedEventId, correlationId);

        PaymentRequestedConsumerService.Result requestedResult =
                requestedConsumerService.handle(bookingId.toString(), paymentRequested);

        assertThat(requestedResult.status())
                .isEqualTo(PaymentRequestedConsumerService.Status.CREATED);

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        assertThat(payment.getSourceEventId()).isEqualTo(paymentRequestedEventId);

        assertThat(payment.getCorrelationId()).isEqualTo(correlationId);

        ClaimedProviderChargeOperation operation = claimChargeOperation(payment);

        ProviderChargeResult providerResult =
                ProviderChargeResult.failed(null, "PAYMENT_DECLINED", "The payment was declined");

        resultApplicationService.apply(operation, providerResult);

        entityManager.clear();

        Payment completedPayment = paymentRepository.findById(payment.getId()).orElseThrow();

        assertThat(completedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(completedPayment.getSourceEventId()).isEqualTo(paymentRequestedEventId);

        assertThat(completedPayment.getCorrelationId()).isEqualTo(correlationId);

        OutboxEventEntity terminalEvent = onlyOutboxEvent();

        assertThat(terminalEvent.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        /*
         * correlationId stays stable for the whole Saga.
         */
        assertThat(terminalEvent.getCorrelationId()).isEqualTo(correlationId);

        /*
         * The immediate Saga event parent remains the original
         * payment-requested event.
         */
        assertThat(terminalEvent.getCausationId()).isEqualTo(paymentRequestedEventId);

        assertThat(terminalEvent.getAggregateId()).isEqualTo(completedPayment.getId());

        assertThat(terminalEvent.getPartitionKey()).isEqualTo(bookingId.toString());
    }

    private OutboxEventMessage paymentRequestedMessage(
            UUID bookingId, UUID eventId, UUID correlationId) {

        OffsetDateTime requestedAt = NOW.minusMinutes(1);

        OffsetDateTime holdExpiresAt = NOW.plusMinutes(10);

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        AMOUNT,
                        CURRENCY,
                        1,
                        holdExpiresAt,
                        requestedAt);

        return new OutboxEventMessage(
                eventId,
                bookingId,
                PaymentEventContract.BOOKING_AGGREGATE_TYPE,
                PaymentEventContract.PAYMENT_REQUESTED,
                PaymentEventContract.PAYMENT_REQUESTED_VERSION,
                requestedAt,
                PaymentEventContract.BOOKING_PRODUCER,
                correlationId,
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private ClaimedProviderChargeOperation claimChargeOperation(Payment payment) {

        payment.startProcessing();

        paymentRepository.saveAndFlush(payment);

        List<PaymentTransaction> transactions =
                transactionRepository.findAllByPaymentIdOrderByAttemptNumberAsc(payment.getId());

        assertThat(transactions).hasSize(1);

        PaymentTransaction transaction = transactions.getFirst();

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.READY);

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(1), NOW.plusSeconds(30));

        transactionRepository.saveAndFlush(transaction);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getHoldExpiresAt());

        return new ClaimedProviderChargeOperation(
                transaction.getId(),
                PROCESSING_OWNER,
                transaction.getProvider(),
                transaction.getIdempotencyKey(),
                command);
    }

    private OutboxEventEntity onlyOutboxEvent() {

        List<OutboxEventEntity> events = outboxRepository.findAll();

        assertThat(events).hasSize(1);

        return events.getFirst();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock paymentTracePropagationClock() {

            return Clock.fixed(CURRENT_INSTANT, ZoneOffset.UTC);
        }
    }
}
