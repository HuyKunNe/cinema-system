package com.cinema.inventory.event.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultBookingConfirmedMessageValidatorTest {

    private static final OffsetDateTime CONFIRMED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final DefaultBookingConfirmedMessageValidator validator =
            new DefaultBookingConfirmedMessageValidator();

    @Test
    void canonicalMessageShouldPassValidation() {

        OutboxEventMessage message = validMessage();

        assertThatCode(() -> validator.validate(message.aggregateId().toString(), message))
                .doesNotThrowAnyException();
    }

    @Test
    void nullMessageShouldBeRejected() {

        assertThatThrownBy(() -> validator.validate("booking-id", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nonUuidV7EventIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        UUID.randomUUID(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void unsupportedEventTypeShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        "unsupported-event",
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void unsupportedEventVersionShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        "2",
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void incorrectProducerShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        "payment-service",
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void incorrectAggregateTypeShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        "PAYMENT",
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void nonUuidV7AggregateIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        UUID.randomUUID(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void incorrectPartitionKeyShouldBeRejected() {

        assertRejected(UuidGenerator.next().toString(), validMessage());
    }

    @Test
    void missingCorrelationIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        null,
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void missingCausationIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        null,
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void missingOccurredAtShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        null,
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload());

        assertRejected(message.aggregateId().toString(), message);
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        source.eventType(),
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        JsonNodeFactory.instance.textNode("invalid"));

        assertRejected(message.aggregateId().toString(), message);
    }

    private void assertRejected(String partitionKey, OutboxEventMessage message) {

        assertThatThrownBy(() -> validator.validate(partitionKey, message))
                .isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage validMessage() {

        UUID bookingId = UuidGenerator.next();

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                InventoryEventContract.BOOKING_CONFIRMED,
                InventoryEventContract.BOOKING_CONFIRMED_VERSION,
                CONFIRMED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode().put("bookingId", bookingId.toString()));
    }

    private OutboxEventMessage copy(
            OutboxEventMessage source,
            UUID eventId,
            UUID aggregateId,
            String aggregateType,
            String eventType,
            String eventVersion,
            OffsetDateTime occurredAt,
            String producer,
            UUID correlationId,
            UUID causationId,
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
                causationId,
                payload);
    }
}
