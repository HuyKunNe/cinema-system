package com.cinema.payment.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonPaymentRequestedMessageReaderTest {

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonPaymentRequestedMessageReader reader =
            new JacksonPaymentRequestedMessageReader(objectMapper);

    @Test
    void shouldReadCanonicalPaymentRequestedMessage() throws Exception {

        OutboxEventMessage expected = validMessage();

        String serializedMessage = objectMapper.writeValueAsString(expected);

        OutboxEventMessage result = reader.read(serializedMessage);

        assertThat(result.eventId()).isEqualTo(expected.eventId());
        assertThat(result.aggregateId()).isEqualTo(BOOKING_ID);
        assertThat(result.aggregateType()).isEqualTo("BOOKING");
        assertThat(result.eventType()).isEqualTo("payment-requested");
        assertThat(result.eventVersion()).isEqualTo("1");
        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(result.producer()).isEqualTo("booking-service");
        assertThat(result.correlationId()).isEqualTo(expected.correlationId());
        assertThat(result.causationId()).isEqualTo(expected.causationId());
        assertThat(result.payload()).isEqualTo(expected.payload());
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertValidation(() -> reader.read(null));
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertValidation(() -> reader.read("   "));
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertValidation(() -> reader.read("{not-valid-json"));
    }

    @Test
    void unknownEnvelopeFieldShouldBeRejected() {

        ObjectNode json = objectMapper.valueToTree(validMessage());

        json.put("unexpectedField", "must-fail");

        assertValidation(() -> reader.read(json.toString()));
    }

    @Test
    void timestampArrayShouldBeRejected() {

        ObjectNode json = objectMapper.valueToTree(validMessage());

        json.set(
                "occurredAt",
                JsonNodeFactory.instance.arrayNode().add(2026).add(8).add(26).add(10).add(0));

        assertValidation(() -> reader.read(json.toString()));
    }

    private static OutboxEventMessage validMessage() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", BOOKING_ID.toString());

        payload.put("userId", UuidGenerator.next().toString());

        payload.put("amount", 180000);

        payload.put("currency", "VND");

        payload.put("paymentAttempt", 1);

        payload.put("holdExpiresAt", "2026-08-26T10:10:00Z");

        payload.put("requestedAt", "2026-08-26T10:00:00Z");

        return new OutboxEventMessage(
                UuidGenerator.next(),
                BOOKING_ID,
                "BOOKING",
                "payment-requested",
                "1",
                OCCURRED_AT,
                "booking-service",
                UuidGenerator.next(),
                UuidGenerator.next(),
                payload);
    }

    private static void assertValidation(ThrowingCallable callable) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(PaymentErrorCode.EVENT_MESSAGE_INVALID));
    }
}
