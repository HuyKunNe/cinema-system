package com.cinema.payment.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentFailedPayloadTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final OffsetDateTime FAILED_AT =
            OffsetDateTime.parse("2026-09-10T09:30:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Test
    void payloadShouldPreserveCanonicalValues() {

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "The payment was declined",
                        FAILED_AT,
                        false);

        assertThat(payload.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(payload.bookingId()).isEqualTo(BOOKING_ID);

        assertThat(payload.failureCode()).isEqualTo("PAYMENT_DECLINED");

        assertThat(payload.message()).isEqualTo("The payment was declined");

        assertThat(payload.failedAt()).isEqualTo(FAILED_AT);

        assertThat(payload.retryable()).isFalse();
    }

    @Test
    void equalPayloadsShouldHaveValueSemantics() {

        PaymentFailedPayload first =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "The payment was declined",
                        FAILED_AT,
                        false);

        PaymentFailedPayload second =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "The payment was declined",
                        FAILED_AT,
                        false);

        assertThat(second).isEqualTo(first);

        assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }

    @Test
    void serializationShouldMatchEventCatalogContract() throws Exception {

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        "PAYMENT_DECLINED",
                        "The payment was declined",
                        FAILED_AT,
                        false);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.size()).isEqualTo(6);

        assertThat(json.get("paymentId").asText()).isEqualTo(PAYMENT_ID.toString());

        assertThat(json.get("bookingId").asText()).isEqualTo(BOOKING_ID.toString());

        assertThat(json.get("failureCode").asText()).isEqualTo("PAYMENT_DECLINED");

        assertThat(json.get("message").asText()).isEqualTo("The payment was declined");

        assertThat(json.get("failedAt").asText()).isEqualTo("2026-09-10T09:30:00Z");

        assertThat(json.get("retryable").asBoolean()).isFalse();

        assertThat(json.has("provider")).isFalse();

        assertThat(json.has("providerReference")).isFalse();

        assertThat(json.has("failureMessage")).isFalse();

        assertThat(json.has("stackTrace")).isFalse();

        assertThat(json.has("providerResponse")).isFalse();

        assertThat(json.has("cardNumber")).isFalse();

        assertThat(json.has("cvv")).isFalse();
    }
}
