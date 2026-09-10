package com.cinema.payment.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentSucceededPayloadTest {

    private static final UUID PAYMENT_ID = UuidGenerator.next();

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final BigDecimal AMOUNT = new BigDecimal("180000.00");

    private static final OffsetDateTime PAID_AT = OffsetDateTime.parse("2026-09-10T09:30:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Test
    void payloadShouldPreserveCanonicalValues() {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        AMOUNT,
                        "VND",
                        "MOMO",
                        "MOMO-TRANSACTION-001",
                        PAID_AT);

        assertThat(payload.paymentId()).isEqualTo(PAYMENT_ID);

        assertThat(payload.bookingId()).isEqualTo(BOOKING_ID);

        assertThat(payload.amount()).isEqualByComparingTo(AMOUNT);

        assertThat(payload.currency()).isEqualTo("VND");

        assertThat(payload.provider()).isEqualTo("MOMO");

        assertThat(payload.providerReference()).isEqualTo("MOMO-TRANSACTION-001");

        assertThat(payload.paidAt()).isEqualTo(PAID_AT);
    }

    @Test
    void equalPayloadsShouldHaveValueSemantics() {

        PaymentSucceededPayload first =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "MOMO-TRANSACTION-001",
                        PAID_AT);

        PaymentSucceededPayload second =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        "MOMO",
                        "MOMO-TRANSACTION-001",
                        PAID_AT);

        assertThat(second).isEqualTo(first);

        assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }

    @Test
    void serializationShouldMatchEventCatalogContract() throws Exception {

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        PAYMENT_ID,
                        BOOKING_ID,
                        AMOUNT,
                        "VND",
                        "MOMO",
                        "MOMO-TRANSACTION-001",
                        PAID_AT);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.size()).isEqualTo(7);

        assertThat(json.get("paymentId").asText()).isEqualTo(PAYMENT_ID.toString());

        assertThat(json.get("bookingId").asText()).isEqualTo(BOOKING_ID.toString());

        assertThat(json.get("amount").decimalValue()).isEqualByComparingTo("180000.00");

        assertThat(json.get("currency").asText()).isEqualTo("VND");

        assertThat(json.get("provider").asText()).isEqualTo("MOMO");

        assertThat(json.get("providerReference").asText()).isEqualTo("MOMO-TRANSACTION-001");

        assertThat(json.get("paidAt").asText()).isEqualTo("2026-09-10T09:30:00Z");

        assertThat(json.has("userId")).isFalse();

        assertThat(json.has("paymentAttempt")).isFalse();

        assertThat(json.has("transactionId")).isFalse();

        assertThat(json.has("cardNumber")).isFalse();

        assertThat(json.has("cvv")).isFalse();

        assertThat(json.has("providerResponse")).isFalse();
    }
}
