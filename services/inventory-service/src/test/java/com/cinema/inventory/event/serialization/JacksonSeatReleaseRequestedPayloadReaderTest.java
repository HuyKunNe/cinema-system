package com.cinema.inventory.event.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class JacksonSeatReleaseRequestedPayloadReaderTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final JacksonSeatReleaseRequestedPayloadReader reader =
            new JacksonSeatReleaseRequestedPayloadReader(objectMapper);

    @Test
    void shouldReadCanonicalSeatReleaseRequestedPayload() {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID firstSeatId = UuidGenerator.next();

        UUID secondSeatId = UuidGenerator.next();

        SeatReleaseRequestedPayload source =
                new SeatReleaseRequestedPayload(
                        bookingId,
                        showtimeId,
                        List.of(firstSeatId, secondSeatId),
                        InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED,
                        REQUESTED_AT);

        OutboxEventMessage message = message(bookingId, objectMapper.valueToTree(source));

        SeatReleaseRequestedPayload result = reader.read(message);

        assertThat(result.bookingId()).isEqualTo(bookingId);

        assertThat(result.showtimeId()).isEqualTo(showtimeId);

        assertThat(result.seatIds()).containsExactly(firstSeatId, secondSeatId);

        assertThat(result.reason())
                .isEqualTo(InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

        assertThat(result.requestedAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertThatThrownBy(() -> reader.read(null)).isInstanceOf(ValidationException.class);
    }

    @Test
    void nullPayloadShouldBeRejected() {

        OutboxEventMessage message = message(UuidGenerator.next(), null);

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        OutboxEventMessage message =
                message(UuidGenerator.next(), JsonNodeFactory.instance.textNode("invalid"));

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    @Test
    void malformedPayloadFieldShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        ObjectNode payload = JsonNodeFactory.instance.objectNode();

        payload.put("bookingId", bookingId.toString());

        payload.put("showtimeId", "not-a-uuid");

        payload.putArray("seatIds").add(UuidGenerator.next().toString());

        payload.put("reason", InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED);

        payload.put("requestedAt", REQUESTED_AT.toString());

        OutboxEventMessage message = message(bookingId, payload);

        assertThatThrownBy(() -> reader.read(message)).isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage message(UUID bookingId, JsonNode payload) {

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                REQUESTED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                payload);
    }
}
