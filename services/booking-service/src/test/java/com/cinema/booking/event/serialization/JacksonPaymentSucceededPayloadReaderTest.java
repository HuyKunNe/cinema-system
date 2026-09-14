package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
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

class JacksonPaymentSucceededPayloadReaderTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final OffsetDateTime PAID_AT = OffsetDateTime.parse("2026-09-14T08:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonPaymentSucceededPayloadReader reader =
            new JacksonPaymentSucceededPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalPaymentSucceededPayload() {

        PaymentSucceededPayload result = reader.read(message(validPayload()));

        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(result.bookingId()).isEqualTo(BOOKING_ID);

        assertThat(result.amount()).isEqualByComparingTo("180000.00");

        assertThat(result.currency()).isEqualTo("VND");

        assertThat(result.provider()).isEqualTo("MOMO");

        assertThat(result.providerReference()).isEqualTo("momo-reference-001");

        assertThat(result.paidAt()).isEqualTo(PAID_AT);
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
    void malformedPaidAtShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.put("paidAt", "not-a-timestamp");

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void paidAtArrayShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.set(
                "paidAt",
                JsonNodeFactory.instance.arrayNode().add(2026).add(9).add(14).add(8).add(0));

        assertPayloadInvalid(() -> reader.read(message(payload)));
    }

    @Test
    void invalidAmountTypeShouldBeRejected() {

        ObjectNode payload = validPayload();

        payload.set("amount", JsonNodeFactory.instance.objectNode().put("value", 180000));

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

        payload.remove("currency");

        PaymentSucceededPayload result = reader.read(message(payload));

        assertThat(result.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(result.currency()).isNull();
    }

    private static ObjectNode validPayload() {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("paymentId", PAYMENT_ID.toString());

        payload.put("bookingId", BOOKING_ID.toString());

        payload.put("amount", 180000.00);

        payload.put("currency", "VND");

        payload.put("provider", "MOMO");

        payload.put("providerReference", "momo-reference-001");

        payload.put("paidAt", PAID_AT.toString());

        return payload;
    }

    private static OutboxEventMessage message(JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                PAYMENT_ID,
                "PAYMENT",
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                PAID_AT,
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
