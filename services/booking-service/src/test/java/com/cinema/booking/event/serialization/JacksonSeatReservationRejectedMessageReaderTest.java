package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonSeatReservationRejectedMessageReaderTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReservationRejectedMessageReader reader =
            new JacksonSeatReservationRejectedMessageReader(objectMapper);

    @Test
    void canonicalMessageShouldBeDeserialized() throws Exception {

        UUID eventId = UuidGenerator.next();

        UUID bookingId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        ObjectNode payload = validPayload(bookingId);

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        bookingId,
                        "BOOKING",
                        "seat-reservation-rejected",
                        "1",
                        OCCURRED_AT,
                        "inventory-service",
                        correlationId,
                        UuidGenerator.next(),
                        payload);

        String serialized = objectMapper.writeValueAsString(source);

        OutboxEventMessage result = reader.read(serialized);

        assertThat(result.eventId()).isEqualTo(eventId);

        assertThat(result.aggregateId()).isEqualTo(bookingId);

        assertThat(result.aggregateType()).isEqualTo("BOOKING");

        assertThat(result.eventType()).isEqualTo("seat-reservation-rejected");

        assertThat(result.eventVersion()).isEqualTo("1");

        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);

        assertThat(result.producer()).isEqualTo("inventory-service");

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.payload()).isEqualTo(payload);

        assertThat(result.payload().get("reasonCode").asText()).isEqualTo("SEAT_UNAVAILABLE");
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertInvalidMessage(null);
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertInvalidMessage(" ");
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventType": "seat-reservation-rejected",
                  "payload":
                }
                """);
    }

    @Test
    void jsonArrayShouldBeRejected() {

        assertInvalidMessage(
                """
                [
                  {
                    "eventType": "seat-reservation-rejected"
                  }
                ]
                """);
    }

    @Test
    void invalidUuidShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventId": "invalid",
                  "aggregateId": "invalid",
                  "aggregateType": "BOOKING",
                  "eventType": "seat-reservation-rejected",
                  "eventVersion": "1",
                  "occurredAt": "2026-08-24T10:00:00Z",
                  "producer": "inventory-service",
                  "correlationId": "invalid",
                  "payload": {}
                }
                """);
    }

    @Test
    void structurallyReadableUnsupportedEventShouldBeReturnedForValidator() {

        String serialized =
                """
                {
                  "aggregateType": "BOOKING",
                  "eventType": "unsupported-event",
                  "eventVersion": "99",
                  "producer": "unknown-service",
                  "payload": {}
                }
                """;

        OutboxEventMessage result = reader.read(serialized);

        assertThat(result.eventType()).isEqualTo("unsupported-event");

        assertThat(result.eventVersion()).isEqualTo("99");

        assertThat(result.producer()).isEqualTo("unknown-service");
    }

    private void assertInvalidMessage(String serializedMessage) {

        assertThatThrownBy(() -> reader.read(serializedMessage))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(BookingErrorCode.EVENT_MESSAGE_INVALID));
    }

    private ObjectNode validPayload(UUID bookingId) {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("showtimeId", UuidGenerator.next().toString());

        payload.put("reasonCode", "SEAT_UNAVAILABLE");

        payload.put("message", "One or more requested seats are unavailable");

        payload.putArray("unavailableSeats").add("H7");

        payload.put("rejectedAt", "2026-08-24T10:00:00Z");

        return payload;
    }
}
