package com.cinema.inventory.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonSeatReservationRequestedMessageReaderTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReservationRequestedMessageReader reader =
            new JacksonSeatReservationRequestedMessageReader(objectMapper);

    @Test
    void shouldReadCanonicalOutboxEventMessage() throws Exception {

        UUID eventId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();
        UUID correlationId = UuidGenerator.next();

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        bookingId,
                        "BOOKING",
                        "seat-reservation-requested",
                        "1",
                        OffsetDateTime.parse("2026-08-24T10:00:00Z"),
                        "booking-service",
                        correlationId,
                        null,
                        JsonNodeFactory.instance
                                .objectNode()
                                .put("bookingId", bookingId.toString()));

        String serialized = objectMapper.writeValueAsString(source);

        OutboxEventMessage result = reader.read(serialized);

        assertThat(result.eventId()).isEqualTo(eventId);

        assertThat(result.aggregateId()).isEqualTo(bookingId);

        assertThat(result.aggregateType()).isEqualTo("BOOKING");

        assertThat(result.eventType()).isEqualTo("seat-reservation-requested");

        assertThat(result.eventVersion()).isEqualTo("1");

        assertThat(result.producer()).isEqualTo("booking-service");

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.payload().get("bookingId").asText()).isEqualTo(bookingId.toString());
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertThrows(ValidationException.class, () -> reader.read(" "));
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertThrows(ValidationException.class, () -> reader.read("{invalid-json"));
    }
}
