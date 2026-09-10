package com.cinema.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.entity.PaymentTransaction;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.PaymentTransactionType;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.event.PaymentSucceededOutboxFactory;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.cinema.payment.provider.model.ClaimedProviderChargeOperation;
import com.cinema.payment.provider.model.ProviderChargeCommand;
import com.cinema.payment.provider.model.ProviderChargeResult;
import com.cinema.payment.provider.model.ProviderOutcome;
import com.cinema.payment.repository.PaymentProviderWebhookEventRepository;
import com.cinema.payment.repository.PaymentRepository;
import com.cinema.payment.repository.PaymentTransactionRepository;
import com.cinema.payment.repository.ProcessedEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Import(PaymentTerminalResultOutboxIntegrationTest.FixedClockConfiguration.class)
@TestPropertySource(
        properties = {
            "cinema.payment.provider=MOCK",
            "cinema.payment.kafka.enabled=false",
            "cinema.outbox.enabled=false"
        })
class PaymentTerminalResultOutboxIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-10T10:00:00Z");

    private static final BigDecimal AMOUNT = new BigDecimal("180000.00");

    private static final String PROCESSING_OWNER = "payment-terminal-outbox-integration-worker";

    @Autowired private PaymentRepository paymentRepository;

    @Autowired private PaymentTransactionRepository transactionRepository;

    @Autowired private PaymentProviderWebhookEventRepository webhookEventRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private PaymentProviderResultApplicationService resultApplicationService;

    @Autowired private PaymentRequestedConsumerService requestedConsumerService;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @MockitoSpyBean private PaymentSucceededOutboxFactory succeededOutboxFactory;

    @BeforeEach
    void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        webhookEventRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        transactionRepository.deleteAllInBatch();

        paymentRepository.deleteAllInBatch();
    }

    @Test
    void providerSuccessShouldPersistPaymentTransactionAndOutboxAtomically() throws Exception {

        ProviderFixture fixture = persistClaimedProviderOperation();

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.SUCCEEDED, "mock-provider-reference-001", null, null, null);

        resultApplicationService.apply(fixture.operation(), providerResult);

        entityManager.clear();

        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        OutboxEventEntity event = onlyOutboxEvent();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.SUCCEEDED);

        assertThat(event.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_SUCCEEDED);

        assertThat(event.getAggregateId()).isEqualTo(payment.getId());

        assertThat(event.getPartitionKey()).isEqualTo(payment.getBookingId().toString());

        assertThat(event.getCorrelationId()).isEqualTo(payment.getCorrelationId());

        assertThat(event.getCausationId()).isEqualTo(payment.getSourceEventId());

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("paymentId").asText()).isEqualTo(payment.getId().toString());

        assertThat(payload.get("bookingId").asText()).isEqualTo(payment.getBookingId().toString());

        assertThat(payload.get("providerReference").asText())
                .isEqualTo("mock-provider-reference-001");
    }

    @Test
    void providerFailureShouldPersistPaymentTransactionAndOutboxAtomically() throws Exception {

        ProviderFixture fixture = persistClaimedProviderOperation();

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.FAILED,
                        null,
                        null,
                        "PAYMENT_DECLINED",
                        "The payment was declined");

        resultApplicationService.apply(fixture.operation(), providerResult);

        entityManager.clear();

        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        OutboxEventEntity event = onlyOutboxEvent();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);

        assertThat(event.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.get("failureCode").asText()).isEqualTo("PAYMENT_DECLINED");

        assertThat(payload.get("retryable").asBoolean()).isFalse();
    }

    @Test
    void expiredRequestShouldPersistMarkerPaymentAndFailureOutboxAtomically() throws Exception {

        UUID bookingId = UuidGenerator.next();

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        OffsetDateTime requestedAt = NOW.minusMinutes(10);

        OffsetDateTime holdExpiresAt = NOW.minusMinutes(1);

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        bookingId,
                        UuidGenerator.next(),
                        AMOUNT,
                        "VND",
                        1,
                        holdExpiresAt,
                        requestedAt);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        eventId,
                        bookingId,
                        "BOOKING",
                        PaymentEventContract.PAYMENT_REQUESTED,
                        PaymentEventContract.PAYMENT_REQUESTED_VERSION,
                        requestedAt,
                        PaymentEventContract.BOOKING_PRODUCER,
                        correlationId,
                        UuidGenerator.next(),
                        objectMapper.valueToTree(payload));

        PaymentRequestedConsumerService.Result result =
                requestedConsumerService.handle(bookingId.toString(), message);

        entityManager.clear();

        Payment payment =
                paymentRepository.findByBookingIdAndPaymentAttempt(bookingId, 1).orElseThrow();

        OutboxEventEntity event = onlyOutboxEvent();

        assertThat(result.status()).isEqualTo(PaymentRequestedConsumerService.Status.EXPIRED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(transactionRepository.count()).isZero();

        assertThat(event.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_FAILED);

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);

        assertThat(event.getCausationId()).isEqualTo(eventId);

        JsonNode eventPayload = objectMapper.readTree(event.getPayload());

        assertThat(eventPayload.get("failureCode").asText()).isEqualTo("RESERVATION_EXPIRED");

        assertThat(eventPayload.get("retryable").asBoolean()).isFalse();
    }

    @Test
    void outboxFailureShouldRollbackProviderSuccessAndTransactionState() {

        ProviderFixture fixture = persistClaimedProviderOperation();

        doThrow(new InternalServerException(PaymentErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED))
                .when(succeededOutboxFactory)
                .create(any(Payment.class));

        ProviderChargeResult providerResult =
                new ProviderChargeResult(
                        ProviderOutcome.SUCCEEDED,
                        "mock-provider-reference-rollback",
                        null,
                        null,
                        null);

        assertThatThrownBy(
                        () -> resultApplicationService.apply(fixture.operation(), providerResult))
                .isInstanceOf(InternalServerException.class);

        entityManager.clear();

        Payment payment = paymentRepository.findById(fixture.paymentId()).orElseThrow();

        PaymentTransaction transaction =
                transactionRepository.findById(fixture.transactionId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);

        assertThat(payment.getProviderReference()).isNull();

        assertThat(transaction.getStatus()).isEqualTo(PaymentTransactionStatus.PROCESSING);

        assertThat(transaction.getProcessingOwner()).isEqualTo(PROCESSING_OWNER);

        assertThat(outboxRepository.count()).isZero();
    }

    private ProviderFixture persistClaimedProviderOperation() {

        UUID bookingId = UuidGenerator.next();

        Payment payment =
                new Payment(
                        bookingId,
                        UuidGenerator.next(),
                        1,
                        AMOUNT,
                        "VND",
                        "MOCK",
                        NOW.plusMinutes(10),
                        NOW.minusMinutes(1),
                        UuidGenerator.next(),
                        UuidGenerator.next());

        payment.startProcessing();

        paymentRepository.saveAndFlush(payment);

        PaymentTransaction transaction =
                new PaymentTransaction(
                        payment.getId(),
                        payment.getProvider(),
                        PaymentTransactionType.CHARGE,
                        1,
                        payment.getAmount(),
                        payment.getCurrency(),
                        "charge:" + payment.getId(),
                        payment.getRequestedAt());

        transaction.claim(PROCESSING_OWNER, NOW.minusSeconds(1), NOW.plusSeconds(30));

        transactionRepository.saveAndFlush(transaction);

        ProviderChargeCommand command =
                new ProviderChargeCommand(
                        payment.getId(),
                        payment.getBookingId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getHoldExpiresAt());

        ClaimedProviderChargeOperation operation =
                new ClaimedProviderChargeOperation(
                        transaction.getId(),
                        PROCESSING_OWNER,
                        transaction.getProvider(),
                        transaction.getIdempotencyKey(),
                        command);

        return new ProviderFixture(payment.getId(), transaction.getId(), operation);
    }

    private OutboxEventEntity onlyOutboxEvent() {

        List<OutboxEventEntity> events = outboxRepository.findAll();

        assertThat(events).hasSize(1);

        return events.getFirst();
    }

    private record ProviderFixture(
            UUID paymentId, UUID transactionId, ClaimedProviderChargeOperation operation) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock paymentTerminalOutboxClock() {

            return Clock.fixed(Instant.parse("2026-09-10T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
