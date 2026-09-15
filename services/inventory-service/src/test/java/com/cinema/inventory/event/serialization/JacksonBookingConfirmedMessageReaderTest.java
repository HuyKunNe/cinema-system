package com.cinema.inventory.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class JacksonBookingConfirmedMessageReaderTest {

    private static final OffsetDateTime CONFIRMED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonBookingConfirmedMessageReader reader =
            new JacksonBookingConfirmedMessageReader(objectMapper);

    @Test
    void shouldReadCanonicalBookingConfirmedMessage() throws Exception {

        UUID eventId = UuidGenerator.next();

        UUID bookingId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        UUID causationId = UuidGenerator.next();

        OutboxEventMessage source =
                new OutboxEventMessage(
                        eventId,
                        bookingId,
                        InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                        InventoryEventContract.BOOKING_CONFIRMED,
                        InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                        CONFIRMED_AT,
                        InventoryEventContract.BOOKING_PRODUCER,
                        correlationId,
                        causationId,
                        JsonNodeFactory.instance
                                .objectNode()
                                .put("bookingId", bookingId.toString()));

        String serialized = objectMapper.writeValueAsString(source);

        OutboxEventMessage result = reader.read(serialized);

        assertThat(result.eventId()).isEqualTo(eventId);

        assertThat(result.aggregateId()).isEqualTo(bookingId);

        assertThat(result.aggregateType()).isEqualTo(InventoryEventContract.BOOKING_AGGREGATE_TYPE);

        assertThat(result.eventType()).isEqualTo(InventoryEventContract.BOOKING_CONFIRMED);

        assertThat(result.eventVersion())
                .isEqualTo(InventoryEventContract.BOOKING_CONFIRMED_VERSION);

        assertThat(result.occurredAt()).isEqualTo(CONFIRMED_AT);

        assertThat(result.producer()).isEqualTo(InventoryEventContract.BOOKING_PRODUCER);

        assertThat(result.correlationId()).isEqualTo(correlationId);

        assertThat(result.causationId()).isEqualTo(causationId);

        assertThat(result.payload().path("bookingId").asText()).isEqualTo(bookingId.toString());
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertThatThrownBy(() -> reader.read(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void blankMessageShouldBeRejected() {

        assertThatThrownBy(() -> reader.read(" ")).isInstanceOf(ValidationException.class);
    }

    @Test
    void malformedJsonShouldBeRejected() {

        assertThatThrownBy(() -> reader.read("{invalid-json"))
                .isInstanceOf(ValidationException.class);
    }
}
