package com.cinema.booking.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.booking.event.payload.SeatReservedPayload;
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

class JacksonSeatReservedPayloadReaderTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReservedPayloadReader reader =
            new JacksonSeatReservedPayloadReader(objectMapper);

    @Test
    void shouldReadSeatReservedPayload() {

        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();
        UUID inventorySeatId = UuidGenerator.next();

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

        OutboxEventMessage message = message(payload);

        SeatReservedPayload result = reader.read(message);

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.seats()).hasSize(1);

        assertThat(result.seats().getFirst().seatNumber()).isEqualTo("H7");

        assertThat(result.totalAmount()).isEqualByComparingTo("90000");

        assertThat(result.currency()).isEqualTo("VND");
    }

    @Test
    void missingPayloadShouldBeRejected() {

        assertThrows(ValidationException.class, () -> reader.read(message(null)));
    }

    @Test
    void malformedPayloadShouldBeRejected() {

        OutboxEventMessage message =
                message(JsonNodeFactory.instance.objectNode().put("bookingId", "not-a-uuid"));

        assertThrows(ValidationException.class, () -> reader.read(message));
    }

    private OutboxEventMessage message(com.fasterxml.jackson.databind.JsonNode payload) {

        UUID bookingId = UuidGenerator.next();

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                "seat-reserved",
                "1",
                OffsetDateTime.parse("2026-08-24T10:00:00Z"),
                "inventory-service",
                UuidGenerator.next(),
                null,
                payload);
    }
}
