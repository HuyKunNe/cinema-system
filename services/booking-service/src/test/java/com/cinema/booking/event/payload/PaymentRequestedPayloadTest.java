package com.cinema.booking.event.payload;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class PaymentRequestedPayloadTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    @Test
    void payloadShouldPreserveCanonicalValues() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        BigDecimal amount = new BigDecimal("180000.00");

        OffsetDateTime holdExpiresAt = OffsetDateTime.parse("2026-08-25T10:10:00Z");

        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-08-25T10:01:00Z");

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        bookingId, userId, amount, "VND", 1, holdExpiresAt, requestedAt);

        assertThat(payload.bookingId()).isEqualTo(bookingId);

        assertThat(payload.userId()).isEqualTo(userId);

        assertThat(payload.amount()).isEqualByComparingTo(amount);

        assertThat(payload.currency()).isEqualTo("VND");

        assertThat(payload.paymentAttempt()).isEqualTo(1);

        assertThat(payload.holdExpiresAt()).isEqualTo(holdExpiresAt);

        assertThat(payload.requestedAt()).isEqualTo(requestedAt);
    }

    @Test
    void equalPayloadsShouldHaveValueEquality() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        OffsetDateTime holdExpiresAt = OffsetDateTime.parse("2026-08-25T10:10:00Z");

        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-08-25T10:01:00Z");

        PaymentRequestedPayload first =
                new PaymentRequestedPayload(
                        bookingId,
                        userId,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        holdExpiresAt,
                        requestedAt);

        PaymentRequestedPayload second =
                new PaymentRequestedPayload(
                        bookingId,
                        userId,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        holdExpiresAt,
                        requestedAt);

        assertThat(second).isEqualTo(first);

        assertThat(second.hashCode()).isEqualTo(first.hashCode());
    }

    @Test
    void serializationShouldMatchEventCatalogContract() throws Exception {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        OffsetDateTime holdExpiresAt = OffsetDateTime.parse("2026-08-25T10:10:00Z");

        OffsetDateTime requestedAt = OffsetDateTime.parse("2026-08-25T10:01:00Z");

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        bookingId,
                        userId,
                        new BigDecimal("180000.00"),
                        "VND",
                        1,
                        holdExpiresAt,
                        requestedAt);

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(payload));

        assertThat(json.size()).isEqualTo(7);

        assertThat(json.get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(json.get("userId").asText()).isEqualTo(userId.toString());

        assertThat(json.get("amount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("180000.00"));

        assertThat(json.get("currency").asText()).isEqualTo("VND");

        assertThat(json.get("paymentAttempt").asInt()).isEqualTo(1);

        assertThat(OffsetDateTime.parse(json.get("holdExpiresAt").asText()))
                .isEqualTo(holdExpiresAt);

        assertThat(OffsetDateTime.parse(json.get("requestedAt").asText())).isEqualTo(requestedAt);

        assertThat(json.has("showtimeId")).isFalse();

        assertThat(json.has("seatNumbers")).isFalse();

        assertThat(json.has("provider")).isFalse();

        assertThat(json.has("providerReference")).isFalse();

        assertThat(json.has("cardNumber")).isFalse();

        assertThat(json.has("cvv")).isFalse();
    }
}
