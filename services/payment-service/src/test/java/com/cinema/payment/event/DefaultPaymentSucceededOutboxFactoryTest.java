package com.cinema.payment.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

class DefaultPaymentSucceededOutboxFactoryTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-10T09:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-09-10T09:30:00Z");

    private static final OffsetDateTime PAID_AT = OffsetDateTime.parse("2026-09-10T09:05:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final DefaultPaymentSucceededOutboxFactory factory =
            new DefaultPaymentSucceededOutboxFactory(objectMapper);

    @Test
    void shouldCreateCanonicalPaymentSucceededOutboxEvent() throws Exception {

        Payment payment = succeededPayment();

        OutboxEventEntity event = factory.create(payment);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType()).isEqualTo(AggregateType.PAYMENT);

        assertThat(event.getAggregateId()).isEqualTo(payment.getId());

        assertThat(event.getEventType()).isEqualTo(PaymentEventContract.PAYMENT_SUCCEEDED);

        assertThat(event.getEventVersion())
                .isEqualTo(PaymentEventContract.PAYMENT_SUCCEEDED_VERSION);

        assertThat(event.getTopic()).isEqualTo(PaymentEventContract.PAYMENT_SUCCEEDED);

        assertThat(event.getPartitionKey()).isEqualTo(payment.getBookingId().toString());

        assertThat(event.getOccurredAt()).isEqualTo(PAID_AT);

        assertThat(event.getCorrelationId()).isEqualTo(payment.getCorrelationId());

        assertThat(event.getCausationId()).isEqualTo(payment.getSourceEventId());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getRetryCount()).isZero();

        assertThat(event.getNextAttemptAt()).isEqualTo(PAID_AT);

        assertThat(event.getCreatedAt()).isEqualTo(PAID_AT);

        JsonNode payload = objectMapper.readTree(event.getPayload());

        assertThat(payload.size()).isEqualTo(7);

        assertThat(payload.get("paymentId").asText()).isEqualTo(payment.getId().toString());

        assertThat(payload.get("bookingId").asText()).isEqualTo(payment.getBookingId().toString());

        assertThat(payload.get("amount").decimalValue()).isEqualByComparingTo("180000.00");

        assertThat(payload.get("currency").asText()).isEqualTo("VND");

        assertThat(payload.get("provider").asText()).isEqualTo("MOMO");

        assertThat(payload.get("providerReference").asText()).isEqualTo("MOMO-TRANSACTION-001");

        assertThat(payload.get("paidAt").asText()).isEqualTo("2026-09-10T09:05:00Z");

        assertThat(payload.has("userId")).isFalse();

        assertThat(payload.has("cardNumber")).isFalse();

        assertThat(payload.has("cvv")).isFalse();

        assertThat(payload.has("providerResponse")).isFalse();
    }

    @Test
    void nonSucceededPaymentShouldBeRejected() {

        Payment payment = newPayment();

        assertThatThrownBy(() -> factory.create(payment))
                .isInstanceOf(ConflictException.class)
                .satisfies(
                        exception ->
                                assertThat(((ConflictException) exception).getErrorCode().code())
                                        .isEqualTo("PAYMENT_RESULT_OUTBOX_NOT_APPLICABLE"));
    }

    @Test
    void nullPaymentShouldUseValidationException() {

        assertThatThrownBy(() -> factory.create(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        exception ->
                                assertThat(((ValidationException) exception).getErrorCode().code())
                                        .isEqualTo("PAYMENT_REQUIRED"));
    }

    @Test
    void serializationFailureShouldUseStablePaymentError() throws Exception {

        ObjectMapper failingMapper = mock(ObjectMapper.class);

        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Serialization failure") {});

        DefaultPaymentSucceededOutboxFactory failingFactory =
                new DefaultPaymentSucceededOutboxFactory(failingMapper);

        assertThatThrownBy(() -> failingFactory.create(succeededPayment()))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        exception ->
                                assertThat(
                                                ((InternalServerException) exception)
                                                        .getErrorCode()
                                                        .code())
                                        .isEqualTo("PAYMENT_OUTBOX_PAYLOAD_SERIALIZATION_FAILED"));
    }

    private static Payment succeededPayment() {

        Payment payment = newPayment();

        payment.startProcessing();

        payment.completeProviderSuccess("MOMO-TRANSACTION-001", PAID_AT);

        return payment;
    }

    private static Payment newPayment() {

        return new Payment(
                UuidGenerator.next(),
                UuidGenerator.next(),
                1,
                new BigDecimal("180000.00"),
                "VND",
                "MOMO",
                HOLD_EXPIRES_AT,
                REQUESTED_AT,
                UuidGenerator.next(),
                UuidGenerator.next());
    }
}
