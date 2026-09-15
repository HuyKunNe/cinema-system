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

class DefaultSeatReleaseRequestedMessageValidatorTest {

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private final DefaultSeatReleaseRequestedMessageValidator validator =
            new DefaultSeatReleaseRequestedMessageValidator();

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

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void unsupportedEventTypeShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
                copy(
                        source,
                        source.eventId(),
                        source.aggregateId(),
                        source.aggregateType(),
                        "booking-confirmed",
                        source.eventVersion(),
                        source.occurredAt(),
                        source.producer(),
                        source.correlationId(),
                        source.causationId(),
                        source.payload()));
    }

    @Test
    void unsupportedEventVersionShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void incorrectProducerShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void incorrectAggregateTypeShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void incorrectPartitionKeyShouldBeRejected() {

        assertRejected(UuidGenerator.next().toString(), validMessage());
    }

    @Test
    void missingCorrelationIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void missingCausationIdShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void missingOccurredAtShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        source.payload()));
    }

    @Test
    void nonObjectPayloadShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        assertRejected(
                source.aggregateId().toString(),
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
                        JsonNodeFactory.instance.textNode("invalid")));
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
                InventoryEventContract.SEAT_RELEASE_REQUESTED,
                InventoryEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                REQUESTED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                JsonNodeFactory.instance.objectNode());
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
