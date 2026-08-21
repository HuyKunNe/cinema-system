package com.cinema.common.outbox.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class OutboxEventMessageTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldExposeCanonicalEventEnvelope() throws Exception {

        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-21T10:15:30Z");

        JsonNode payload =
                objectMapper.readTree(
                        """
                        {
                          "bookingId": "019c1234-1111-7abc-8def-0123456789ab",
                          "seatNumbers": ["H7", "H8"]
                        }
                        """);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        eventId,
                        aggregateId,
                        "BOOKING",
                        "seat-reservation-requested",
                        "1",
                        occurredAt,
                        "booking-service",
                        correlationId,
                        causationId,
                        payload);

        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.aggregateId()).isEqualTo(aggregateId);
        assertThat(message.aggregateType()).isEqualTo("BOOKING");
        assertThat(message.eventType()).isEqualTo("seat-reservation-requested");
        assertThat(message.eventVersion()).isEqualTo("1");
        assertThat(message.occurredAt()).isEqualTo(occurredAt);
        assertThat(message.createdAt()).isEqualTo(occurredAt);
        assertThat(message.producer()).isEqualTo("booking-service");
        assertThat(message.correlationId()).isEqualTo(correlationId);
        assertThat(message.causationId()).isEqualTo(causationId);
        assertThat(message.payload()).isEqualTo(payload);
    }
}
