package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonSeatReservedMessageReaderTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReservedMessageReader reader =
            new JacksonSeatReservedMessageReader(objectMapper);

    @Test
    void canonicalMessageShouldBeDeserialized() throws Exception {

        UUID eventId = UuidGenerator.next();

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID inventorySeatId = UuidGenerator.next();

        ObjectNode payload = payload(bookingId, showtimeId, inventorySeatId);

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        bookingId,
                        "BOOKING",
                        "seat-reserved",
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

        assertThat(result.eventType()).isEqualTo("seat-reserved");

        assertThat(result.eventVersion()).isEqualTo("1");

        assertThat(result.occurredAt()).isEqualTo(OCCURRED_AT);

        assertThat(result.producer()).isEqualTo("inventory-service");

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.causationId()).isEqualTo(source.causationId());

        assertThat(result.payload()).isEqualTo(payload);

        assertThat(result.payload().get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(result.payload().get("seats").get(0).get("seatNumber").asText()).isEqualTo("H7");
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertInvalidMessage(null);
    }

    @Test
    void emptyMessageShouldBeRejected() {

        assertInvalidMessage("");
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertInvalidMessage("   ");
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventType": "seat-reserved",
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
                    "eventType": "seat-reserved"
                  }
                ]
                """);
    }

    @Test
    void incompatibleUuidShouldBeRejected() {

        assertInvalidMessage(
                """
                {
                  "eventId": "not-a-uuid",
                  "aggregateId": "not-a-uuid",
                  "aggregateType": "BOOKING",
                  "eventType": "seat-reserved",
                  "eventVersion": "1",
                  "occurredAt": "2026-08-24T10:00:00Z",
                  "producer": "inventory-service",
                  "correlationId": "not-a-uuid",
                  "payload": {}
                }
                """);
    }

    @Test
    void incompatibleTimestampShouldBeRejected() {

        UUID eventId = UuidGenerator.next();

        UUID bookingId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        assertInvalidMessage(
                """
                {
                  "eventId": "%s",
                  "aggregateId": "%s",
                  "aggregateType": "BOOKING",
                  "eventType": "seat-reserved",
                  "eventVersion": "1",
                  "occurredAt": "not-a-timestamp",
                  "producer": "inventory-service",
                  "correlationId": "%s",
                  "payload": {}
                }
                """
                        .formatted(eventId, bookingId, correlationId));
    }

    @Test
    void structurallyReadableMessageShouldBeReturnedForValidator() {

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

    private ObjectNode payload(UUID bookingId, UUID showtimeId, UUID inventorySeatId) {

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("showtimeId", showtimeId.toString());

        ArrayNode seats = JsonNodeFactory.instance.arrayNode();

        ObjectNode seat = JsonNodeFactory.instance.objectNode();

        seat.put("inventorySeatId", inventorySeatId.toString());

        seat.put("seatNumber", "H7");

        seat.put("seatType", "STANDARD");

        seat.put("price", 90000);

        seats.add(seat);

        payload.set("seats", seats);

        payload.put("totalAmount", 90000);

        payload.put("currency", "VND");

        payload.put("heldAt", "2026-08-24T10:00:00Z");

        payload.put("holdExpiresAt", "2026-08-24T10:10:00Z");

        return payload;
    }
}
