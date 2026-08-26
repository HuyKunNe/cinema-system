package com.cinema.payment.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonPaymentRequestedPayloadReaderTest {

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final UUID USER_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonPaymentRequestedPayloadReader reader =
            new JacksonPaymentRequestedPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalPaymentRequestedPayload() {

        PaymentRequestedPayload result = reader.read(message(validPayload()));

        assertThat(result.bookingId()).isEqualTo(BOOKING_ID);
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.amount()).isEqualByComparingTo("180000.00");
        assertThat(result.currency()).isEqualTo("VND");
        assertThat(result.paymentAttempt()).isEqualTo(1);
        assertThat(result.holdExpiresAt()).isEqualTo(REQUESTED_AT.plusMinutes(10));
        assertThat(result.requestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertValidation(() -> reader.read(null));
    }

    @Test
    void nullPayloadShouldBeRejected() {

        assertValidation(() -> reader.read(message(null)));
    }

    @Test
    void arrayPayloadShouldBeRejected() {

        assertValidation(() -> reader.read(message(JsonNodeFactory.instance.arrayNode())));
    }

    @Test
    void malformedBookingIdShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("bookingId", "not-a-uuid");

        assertValidation(() -> reader.read(message(payload)));
    }

    @Test
    void malformedTimestampShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("holdExpiresAt", "not-a-timestamp");

        assertValidation(() -> reader.read(message(payload)));
    }

    @Test
    void timestampArrayShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.set(
                "requestedAt",
                JsonNodeFactory.instance.arrayNode().add(2026).add(8).add(26).add(10).add(0));

        assertValidation(() -> reader.read(message(payload)));
    }

    @Test
    void unknownPayloadFieldShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("unexpectedField", "must-fail");

        assertValidation(() -> reader.read(message(payload)));
    }

    private static ObjectNode validPayload() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", BOOKING_ID.toString());

        payload.put("userId", USER_ID.toString());

        payload.put("amount", 180000.00);

        payload.put("currency", "VND");

        payload.put("paymentAttempt", 1);

        payload.put("holdExpiresAt", REQUESTED_AT.plusMinutes(10).toString());

        payload.put("requestedAt", REQUESTED_AT.toString());

        return payload;
    }

    private static OutboxEventMessage message(JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                BOOKING_ID,
                "BOOKING",
                "payment-requested",
                "1",
                REQUESTED_AT,
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
                                        .isEqualTo(PaymentErrorCode.EVENT_PAYLOAD_INVALID));
    }
}
