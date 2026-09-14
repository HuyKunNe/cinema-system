package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonPaymentResultMessageReaderTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonPaymentResultMessageReader reader =
            new JacksonPaymentResultMessageReader(objectMapper);

    @Test
    void shouldReadCanonicalPaymentSucceededEnvelope() throws Exception {

        UUID eventId = UuidGenerator.next();
        UUID paymentId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("paymentId", paymentId.toString());
        payload.put("bookingId", bookingId.toString());
        payload.put("amount", 180000.00);
        payload.put("currency", "VND");
        payload.put("provider", "MOMO");
        payload.put("providerReference", "momo-reference-001");
        payload.put("paidAt", OCCURRED_AT.toString());

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        paymentId,
                        "PAYMENT",
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                        OCCURRED_AT,
                        BookingEventContract.PAYMENT_PRODUCER,
                        correlationId,
                        causationId,
                        payload);

        String serializedMessage = objectMapper.writeValueAsString(source);

        OutboxEventMessage result = reader.read(serializedMessage);

        assertThat(result.eventId()).isEqualTo(eventId);

        assertThat(result.aggregateId()).isEqualTo(paymentId);

        assertThat(result.aggregateType()).isEqualTo("PAYMENT");

        assertThat(result.eventType()).isEqualTo(BookingEventContract.PAYMENT_SUCCEEDED);

        assertThat(result.eventVersion()).isEqualTo(BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);

        assertThat(result.producer()).isEqualTo(BookingEventContract.PAYMENT_PRODUCER);

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.causationId()).isEqualTo(causationId);

        assertThat(result.payload()).isEqualTo(payload);
    }

    @Test
    void shouldReadCanonicalPaymentFailedEnvelope() throws Exception {

        UUID eventId = UuidGenerator.next();
        UUID paymentId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("paymentId", paymentId.toString());
        payload.put("bookingId", bookingId.toString());
        payload.put("failureCode", "PAYMENT_DECLINED");
        payload.put("message", "Payment was declined");
        payload.put("failedAt", OCCURRED_AT.toString());
        payload.put("retryable", false);

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        paymentId,
                        "PAYMENT",
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION,
                        OCCURRED_AT,
                        BookingEventContract.PAYMENT_PRODUCER,
                        correlationId,
                        causationId,
                        payload);

        String serializedMessage = objectMapper.writeValueAsString(source);

        OutboxEventMessage result = reader.read(serializedMessage);

        assertThat(result.eventId()).isEqualTo(eventId);

        assertThat(result.aggregateId()).isEqualTo(paymentId);

        assertThat(result.aggregateType()).isEqualTo("PAYMENT");

        assertThat(result.eventType()).isEqualTo(BookingEventContract.PAYMENT_FAILED);

        assertThat(result.eventVersion()).isEqualTo(BookingEventContract.PAYMENT_FAILED_VERSION);

        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);

        assertThat(result.producer()).isEqualTo(BookingEventContract.PAYMENT_PRODUCER);

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.causationId()).isEqualTo(causationId);

        assertThat(result.payload()).isEqualTo(payload);
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertInvalidMessage(null);
    }

    @Test
    void emptyMessageShouldBeRejected() {

        assertInvalidMessage("");
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertInvalidMessage("   ");
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventType": "payment-succeeded",
                  "payload":
                }
                """);
    }

    @Test
    void jsonArrayShouldBeRejected() {

        assertInvalidMessage(
                """
                [
                  {
                    "eventType": "payment-succeeded"
                  }
                ]
                """);
    }

    @Test
    void incompatibleUuidShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventId": "not-a-uuid",
                  "aggregateId": "not-a-uuid",
                  "aggregateType": "PAYMENT",
                  "eventType": "payment-succeeded",
                  "eventVersion": "1",
                  "occurredAt": "2026-09-14T08:00:00Z",
                  "producer": "payment-service",
                  "correlationId": "not-a-uuid",
                  "causationId": "not-a-uuid",
                  "payload": {}
                }
                """);
    }

    @Test
    void incompatibleTimestampShouldBeRejected() {

        UUID eventId = UuidGenerator.next();
        UUID paymentId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        assertInvalidMessage(
                """
                {
                  "eventId": "%s",
                  "aggregateId": "%s",
                  "aggregateType": "PAYMENT",
                  "eventType": "payment-succeeded",
                  "eventVersion": "1",
                  "occurredAt": "not-a-timestamp",
                  "producer": "payment-service",
                  "correlationId": "%s",
                  "causationId": "%s",
                  "payload": {}
                }
                """
                        .formatted(eventId, paymentId, correlationId, causationId));
    }

    @Test
    void unknownEnvelopeFieldShouldBeRejected() {

        UUID eventId = UuidGenerator.next();
        UUID paymentId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();
        UUID causationId = UuidGenerator.next();

        assertInvalidMessage(
                """
                {
                  "eventId": "%s",
                  "aggregateId": "%s",
                  "aggregateType": "PAYMENT",
                  "eventType": "payment-succeeded",
                  "eventVersion": "1",
                  "occurredAt": "2026-09-14T08:00:00Z",
                  "producer": "payment-service",
                  "correlationId": "%s",
                  "causationId": "%s",
                  "payload": {},
                  "unexpectedField": "must-fail"
                }
                """
                        .formatted(eventId, paymentId, correlationId, causationId));
    }

    @Test
    void structurallyReadableEnvelopeShouldBeReturnedForValidator() {

        String serializedMessage =
                """
                {
                  "aggregateType": "PAYMENT",
                  "eventType": "unsupported-payment-result",
                  "eventVersion": "99",
                  "producer": "unknown-service",
                  "payload": {}
                }
                """;

        OutboxEventMessage result = reader.read(serializedMessage);

        assertThat(result.aggregateType()).isEqualTo("PAYMENT");

        assertThat(result.eventType()).isEqualTo("unsupported-payment-result");

        assertThat(result.eventVersion()).isEqualTo("99");

        assertThat(result.producer()).isEqualTo("unknown-service");
    }

    private void assertInvalidMessage(String serializedMessage) {

        assertValidation(
                () -> reader.read(serializedMessage), BookingErrorCode.EVENT_MESSAGE_INVALID);
    }

    private static void assertValidation(
            ThrowingCallable callable, BookingErrorCode expectedErrorCode) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(expectedErrorCode));
    }
}
