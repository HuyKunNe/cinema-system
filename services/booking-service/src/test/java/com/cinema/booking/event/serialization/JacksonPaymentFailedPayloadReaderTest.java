package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonPaymentFailedPayloadReaderTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final OffsetDateTime FAILED_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonPaymentFailedPayloadReader reader =
            new JacksonPaymentFailedPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalPaymentFailedPayload() {

        PaymentFailedPayload result = reader.read(message(validPayload()));

        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(result.bookingId()).isEqualTo(BOOKING_ID);

        assertThat(result.failureCode()).isEqualTo("PAYMENT_DECLINED");

        assertThat(result.message()).isEqualTo("Payment was declined");

        assertThat(result.failedAt()).isEqualTo(FAILED_AT);

        assertThat(result.retryable()).isFalse();
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertPayloadInvalid(() -> reader.read(null));
    }

    @Test
    void nullPayloadShouldBeRejected() {

        assertPayloadInvalid(() -> reader.read(message(null)));
    }

    @Test
    void nullJsonPayloadShouldBeRejected() {

        assertPayloadInvalid(() -> reader.read(message(JsonNodeFactory.instance.nullNode())));
    }

    @Test
    void arrayPayloadShouldBeRejected() {

        assertPayloadInvalid(() -> reader.read(message(JsonNodeFactory.instance.arrayNode())));
    }

    @Test
    void scalarPayloadShouldBeRejected() {

        assertPayloadInvalid(
                () -> reader.read(message(JsonNodeFactory.instance.textNode("invalid-payload"))));
    }

    @Test
    void malformedPaymentIdShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("paymentId", "not-a-uuid");

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void malformedBookingIdShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("bookingId", "not-a-uuid");

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void malformedFailedAtShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("failedAt", "not-a-timestamp");

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void failedAtArrayShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.set(
                "failedAt",
                JsonNodeFactory.instance.arrayNode().add(2026).add(9).add(14).add(8).add(0));

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void invalidRetryableTypeShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.set("retryable", JsonNodeFactory.instance.objectNode().put("value", false));

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void unknownPayloadFieldShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("unexpectedField", "must-fail");

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void structurallyReadablePayloadShouldBeReturnedForValidator() {

        ObjectNode payload = validPayload();

        payload.remove("failureCode");

        PaymentFailedPayload result = reader.read(message(payload));

        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(result.failureCode()).isNull();
    }

    private static ObjectNode validPayload() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("paymentId", PAYMENT_ID.toString());

        payload.put("bookingId", BOOKING_ID.toString());

        payload.put("failureCode", "PAYMENT_DECLINED");

        payload.put("message", "Payment was declined");

        payload.put("failedAt", FAILED_AT.toString());

        payload.put("retryable", false);

        return payload;
    }

    private static OutboxEventMessage message(JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                PAYMENT_ID,
                "PAYMENT",
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION,
                FAILED_AT,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                payload);
    }

    private static void assertPayloadInvalid(ThrowingCallable callable) {

        assertThatThrownBy(callable)
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.EVENT_PAYLOAD_INVALID));
    }
}
