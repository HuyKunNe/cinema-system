package com.cinema.payment.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentRequestedPayloadTest {

    private static final UUID BOOKING_ID = UuidGenerator.next();

    private static final UUID USER_ID = UuidGenerator.next();

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-08-26T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-08-26T10:10:00Z");

    @Test
    void shouldExposeCanonicalPayloadFields() {

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        assertThat(payload.bookingId()).isEqualTo(BOOKING_ID);

        assertThat(payload.userId()).isEqualTo(USER_ID);

        assertThat(payload.amount()).isEqualByComparingTo("180000.00");

        assertThat(payload.currency()).isEqualTo("VND");

        assertThat(payload.paymentAttempt()).isEqualTo(1);

        assertThat(payload.holdExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);

        assertThat(payload.requestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void equalPayloadsShouldHaveEqualValueSemantics() {

        PaymentRequestedPayload first =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        PaymentRequestedPayload second =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        assertThat(first).isEqualTo(second);

        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void shouldSerializeWithCanonicalFieldNames() throws Exception {

        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .addModule(new JavaTimeModule())
                        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                        .build();

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        BOOKING_ID,
                        USER_ID,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        HOLD_EXPIRES_AT,
                        REQUESTED_AT);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.size()).isEqualTo(7);
        assertThat(json.get("bookingId").asText()).isEqualTo(BOOKING_ID.toString());
        assertThat(json.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(json.get("amount").decimalValue()).isEqualByComparingTo("180000.00");
        assertThat(json.get("currency").asText()).isEqualTo("VND");
        assertThat(json.get("paymentAttempt").asInt()).isEqualTo(1);
        assertThat(json.get("holdExpiresAt").asText()).isEqualTo("2026-08-26T10:10:00Z");
        assertThat(json.get("requestedAt").asText()).isEqualTo("2026-08-26T10:00:00Z");
    }
}
