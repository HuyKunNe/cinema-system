package com.cinema.booking.event.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultSeatReservationRejectedMessageValidatorTest {

    private static final String EVENT_TYPE = "seat-reservation-rejected";

    private static final String EVENT_VERSION = "1";

    private static final String PRODUCER = "inventory-service";

    private static final String AGGREGATE_TYPE = "BOOKING";

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private final DefaultSeatReservationRejectedMessageValidator validator =
            new DefaultSeatReservationRejectedMessageValidator();

    @Test
    void validMessageShouldPassValidation() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = validMessage(bookingId);

        assertDoesNotThrow(() -> validator.validate(bookingId.toString(), message));
    }

    @Test
    void missingMessageShouldBeRejected() {

        assertThrows(
                ValidationException.class,
                () -> validator.validate(UuidGenerator.next().toString(), null));
    }

    @Test
    void missingEventIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        null,
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void nonUuidV7EventIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UUID.randomUUID(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void unsupportedEventTypeShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        "seat-reserved",
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void unsupportedEventVersionShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        "2",
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void unexpectedProducerShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        "booking-service",
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void unexpectedAggregateTypeShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        "INVENTORY",
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void missingAggregateIdShouldBeRejected() {

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        null,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(UuidGenerator.next()));

        assertRejected(UuidGenerator.next().toString(), message);
    }

    @Test
    void partitionKeyMismatchShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message = validMessage(bookingId);

        assertRejected(UuidGenerator.next().toString(), message);
    }

    @Test
    void missingPartitionKeyShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        assertRejected(null, validMessage(bookingId));
    }

    @Test
    void missingCorrelationIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        null,
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void nonUuidV7CorrelationIdShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UUID.randomUUID(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void missingOccurredAtShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        null,
                        PRODUCER,
                        UuidGenerator.next(),
                        validPayload(bookingId));

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void missingPayloadShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        null);

        assertRejected(bookingId.toString(), message);
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        UUID bookingId = UuidGenerator.next();

        JsonNode payload = JsonNodeFactory.instance.textNode("invalid-payload");

        OutboxEventMessage message =
                message(
                        UuidGenerator.next(),
                        bookingId,
                        AGGREGATE_TYPE,
                        EVENT_TYPE,
                        EVENT_VERSION,
                        OCCURRED_AT,
                        PRODUCER,
                        UuidGenerator.next(),
                        payload);

        assertRejected(bookingId.toString(), message);
    }

    private void assertRejected(String partitionKey, OutboxEventMessage message) {

        assertThrows(ValidationException.class, () -> validator.validate(partitionKey, message));
    }

    private OutboxEventMessage validMessage(UUID bookingId) {

        return message(
                UuidGenerator.next(),
                bookingId,
                AGGREGATE_TYPE,
                EVENT_TYPE,
                EVENT_VERSION,
                OCCURRED_AT,
                PRODUCER,
                UuidGenerator.next(),
                validPayload(bookingId));
    }

    private OutboxEventMessage message(
            UUID eventId,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            UUID correlationId,
            JsonNode payload) {

        return new OutboxEventMessage(
                eventId,
                aggregateId,
                aggregateType,
                eventType,
                eventVersion,
                occurredAt,
                producer,
                correlationId,
                null,
                payload);
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
