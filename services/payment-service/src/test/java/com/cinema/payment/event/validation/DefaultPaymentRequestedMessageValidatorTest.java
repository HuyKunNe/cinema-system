package com.cinema.payment.event.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultPaymentRequestedMessageValidatorTest {

    private static final UUID EVENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final UUID CORRELATION_ID = UuidGenerator.next();

    private static final UUID CAUSATION_ID = UuidGenerator.next();

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final JsonNode PAYLOAD = JsonNodeFactory.instance.objectNode();

    private final DefaultPaymentRequestedMessageValidator validator =
            new DefaultPaymentRequestedMessageValidator();

    @Test
    void canonicalMessageShouldBeAccepted() {

        assertThatCode(
                        () ->
                                validator.validate(
                                        BOOKING_ID.toString(),
                                        message(
                                                EVENT_ID,
                                                BOOKING_ID,
                                                "BOOKING",
                                                "payment-requested",
                                                "1",
                                                OCCURRED_AT,
                                                "booking-service",
                                                CORRELATION_ID,
                                                CAUSATION_ID,
                                                PAYLOAD)))
                .doesNotThrowAnyException();
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertValidation(
                () -> validator.validate(BOOKING_ID.toString(), null),
                PaymentErrorCode.EVENT_MESSAGE_INVALID);
    }

    @Test
    void missingEventIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        null,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_ID_REQUIRED);
    }

    @Test
    void nonUuidV7EventIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        UUID.randomUUID(),
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_ID_INVALID);
    }

    @Test
    void wrongEventTypeShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-succeeded",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_TYPE_INVALID);
    }

    @Test
    void wrongEventVersionShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "2",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_VERSION_INVALID);
    }

    @Test
    void wrongProducerShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "inventory-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_PRODUCER_INVALID);
    }

    @Test
    void wrongAggregateTypeShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "PAYMENT",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_AGGREGATE_INVALID);
    }

    @Test
    void nonUuidV7AggregateIdShouldBeRejected() {

        UUID aggregateId = UUID.randomUUID();

        assertValidation(
                () ->
                        validator.validate(
                                aggregateId.toString(),
                                message(
                                        EVENT_ID,
                                        aggregateId,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_AGGREGATE_INVALID);
    }

    @Test
    void partitionKeyMismatchShouldBeRejected() {

        assertValidation(
                () -> validator.validate(UuidGenerator.next().toString(), validMessage()),
                PaymentErrorCode.EVENT_PARTITION_KEY_INVALID);
    }

    @Test
    void missingCorrelationIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        null,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_CORRELATION_ID_INVALID);
    }

    @Test
    void nonUuidV7CorrelationIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        UUID.randomUUID(),
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_CORRELATION_ID_INVALID);
    }

    @Test
    void missingCausationIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        null,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_CAUSATION_ID_REQUIRED);
    }

    @Test
    void nonUuidV7CausationIdShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        UUID.randomUUID(),
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_CAUSATION_ID_INVALID);
    }

    @Test
    void missingOccurredAtShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        null,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        PAYLOAD)),
                PaymentErrorCode.EVENT_OCCURRED_AT_REQUIRED);
    }

    @Test
    void nullPayloadShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        null)),
                PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }

    @Test
    void arrayPayloadShouldBeRejected() {

        assertValidation(
                () ->
                        validator.validate(
                                BOOKING_ID.toString(),
                                message(
                                        EVENT_ID,
                                        BOOKING_ID,
                                        "BOOKING",
                                        "payment-requested",
                                        "1",
                                        OCCURRED_AT,
                                        "booking-service",
                                        CORRELATION_ID,
                                        CAUSATION_ID,
                                        JsonNodeFactory.instance.arrayNode())),
                PaymentErrorCode.EVENT_PAYLOAD_INVALID);
    }

    private static OutboxEventMessage validMessage() {

        return message(
                EVENT_ID,
                BOOKING_ID,
                "BOOKING",
                "payment-requested",
                "1",
                OCCURRED_AT,
                "booking-service",
                CORRELATION_ID,
                CAUSATION_ID,
                PAYLOAD);
    }

    private static OutboxEventMessage message(
            UUID eventId,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            UUID correlationId,
            UUID causationId,
            JsonNode payload) {

        return new OutboxEventMessage(
                eventId,
                aggregateId,
                aggregateType,
                eventType,
                eventVersion,
                occurredAt,
                producer,
                correlationId,
                causationId,
                payload);
    }

    private static void assertValidation(
            ThrowingCallable callable, PaymentErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}
