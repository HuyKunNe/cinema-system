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

class BookingLifecycleMessageValidatorTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.parse("2026-09-16T08:30:00Z");

    private final DefaultBookingCancelledMessageValidator cancelledValidator =
            new DefaultBookingCancelledMessageValidator();

    private final DefaultBookingExpiredMessageValidator expiredValidator =
            new DefaultBookingExpiredMessageValidator();

    @Test
    void canonicalCancelledMessageWithoutCausationShouldPass() {

        OutboxEventMessage message = cancelledMessage(null);

        assertThatCode(() -> cancelledValidator.validate(message.aggregateId().toString(), message))
                .doesNotThrowAnyException();
    }

    @Test
    void canonicalExpiredMessageWithoutCausationShouldPass() {

        OutboxEventMessage message = expiredMessage(null);

        assertThatCode(() -> expiredValidator.validate(message.aggregateId().toString(), message))
                .doesNotThrowAnyException();
    }

    @Test
    void lifecycleMessageWithUuidV7CausationShouldPass() {

        OutboxEventMessage cancelled = cancelledMessage(UuidGenerator.next());

        OutboxEventMessage expired = expiredMessage(UuidGenerator.next());

        assertThatCode(
                        () ->
                                cancelledValidator.validate(
                                        cancelled.aggregateId().toString(), cancelled))
                .doesNotThrowAnyException();

        assertThatCode(() -> expiredValidator.validate(expired.aggregateId().toString(), expired))
                .doesNotThrowAnyException();
    }

    @Test
    void nullMessagesShouldBeRejected() {

        assertThatThrownBy(() -> cancelledValidator.validate("booking-id", null))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> expiredValidator.validate("booking-id", null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void nonUuidV7EventIdsShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        OutboxEventMessage expired = expiredMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        UUID.randomUUID(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        cancelled.eventType(),
                        cancelled.eventVersion(),
                        cancelled.occurredAt(),
                        cancelled.producer(),
                        cancelled.correlationId(),
                        cancelled.causationId(),
                        cancelled.payload()));

        assertExpiredRejected(
                copy(
                        expired,
                        UUID.randomUUID(),
                        expired.aggregateId(),
                        expired.aggregateType(),
                        expired.eventType(),
                        expired.eventVersion(),
                        expired.occurredAt(),
                        expired.producer(),
                        expired.correlationId(),
                        expired.causationId(),
                        expired.payload()));
    }

    @Test
    void wrongLifecycleEventTypesShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        OutboxEventMessage expired = expiredMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        cancelled.eventId(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        InventoryEventContract.BOOKING_EXPIRED,
                        cancelled.eventVersion(),
                        cancelled.occurredAt(),
                        cancelled.producer(),
                        cancelled.correlationId(),
                        cancelled.causationId(),
                        cancelled.payload()));

        assertExpiredRejected(
                copy(
                        expired,
                        expired.eventId(),
                        expired.aggregateId(),
                        expired.aggregateType(),
                        InventoryEventContract.BOOKING_CANCELLED,
                        expired.eventVersion(),
                        expired.occurredAt(),
                        expired.producer(),
                        expired.correlationId(),
                        expired.causationId(),
                        expired.payload()));
    }

    @Test
    void unsupportedVersionsShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        OutboxEventMessage expired = expiredMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        cancelled.eventId(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        cancelled.eventType(),
                        "2",
                        cancelled.occurredAt(),
                        cancelled.producer(),
                        cancelled.correlationId(),
                        cancelled.causationId(),
                        cancelled.payload()));

        assertExpiredRejected(
                copy(
                        expired,
                        expired.eventId(),
                        expired.aggregateId(),
                        expired.aggregateType(),
                        expired.eventType(),
                        "2",
                        expired.occurredAt(),
                        expired.producer(),
                        expired.correlationId(),
                        expired.causationId(),
                        expired.payload()));
    }

    @Test
    void incorrectProducerAndAggregateShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        cancelled.eventId(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        cancelled.eventType(),
                        cancelled.eventVersion(),
                        cancelled.occurredAt(),
                        "payment-service",
                        cancelled.correlationId(),
                        cancelled.causationId(),
                        cancelled.payload()));

        OutboxEventMessage expired = expiredMessage(null);

        assertExpiredRejected(
                copy(
                        expired,
                        expired.eventId(),
                        expired.aggregateId(),
                        "PAYMENT",
                        expired.eventType(),
                        expired.eventVersion(),
                        expired.occurredAt(),
                        expired.producer(),
                        expired.correlationId(),
                        expired.causationId(),
                        expired.payload()));
    }

    @Test
    void incorrectPartitionKeysShouldBeRejected() {

        assertThatThrownBy(
                        () ->
                                cancelledValidator.validate(
                                        UuidGenerator.next().toString(), cancelledMessage(null)))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(
                        () ->
                                expiredValidator.validate(
                                        UuidGenerator.next().toString(), expiredMessage(null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void missingCorrelationIdsShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        OutboxEventMessage expired = expiredMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        cancelled.eventId(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        cancelled.eventType(),
                        cancelled.eventVersion(),
                        cancelled.occurredAt(),
                        cancelled.producer(),
                        null,
                        cancelled.causationId(),
                        cancelled.payload()));

        assertExpiredRejected(
                copy(
                        expired,
                        expired.eventId(),
                        expired.aggregateId(),
                        expired.aggregateType(),
                        expired.eventType(),
                        expired.eventVersion(),
                        expired.occurredAt(),
                        expired.producer(),
                        null,
                        expired.causationId(),
                        expired.payload()));
    }

    @Test
    void nonUuidV7CausationIdsShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(UUID.randomUUID());

        OutboxEventMessage expired = expiredMessage(UUID.randomUUID());

        assertCancelledRejected(cancelled);

        assertExpiredRejected(expired);
    }

    @Test
    void missingOccurredAtAndNonObjectPayloadShouldBeRejected() {

        OutboxEventMessage cancelled = cancelledMessage(null);

        assertCancelledRejected(
                copy(
                        cancelled,
                        cancelled.eventId(),
                        cancelled.aggregateId(),
                        cancelled.aggregateType(),
                        cancelled.eventType(),
                        cancelled.eventVersion(),
                        null,
                        cancelled.producer(),
                        cancelled.correlationId(),
                        cancelled.causationId(),
                        cancelled.payload()));

        OutboxEventMessage expired = expiredMessage(null);

        assertExpiredRejected(
                copy(
                        expired,
                        expired.eventId(),
                        expired.aggregateId(),
                        expired.aggregateType(),
                        expired.eventType(),
                        expired.eventVersion(),
                        expired.occurredAt(),
                        expired.producer(),
                        expired.correlationId(),
                        expired.causationId(),
                        JsonNodeFactory.instance.textNode("invalid")));
    }

    private void assertCancelledRejected(OutboxEventMessage message) {

        assertThatThrownBy(
                        () ->
                                cancelledValidator.validate(
                                        message.aggregateId().toString(), message))
                .isInstanceOf(ValidationException.class);
    }

    private void assertExpiredRejected(OutboxEventMessage message) {

        assertThatThrownBy(
                        () -> expiredValidator.validate(message.aggregateId().toString(), message))
                .isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage cancelledMessage(UUID causationId) {

        return message(
                InventoryEventContract.BOOKING_CANCELLED,
                InventoryEventContract.BOOKING_CANCELLED_VERSION,
                causationId);
    }

    private OutboxEventMessage expiredMessage(UUID causationId) {

        return message(
                InventoryEventContract.BOOKING_EXPIRED,
                InventoryEventContract.BOOKING_EXPIRED_VERSION,
                causationId);
    }

    private OutboxEventMessage message(String eventType, String eventVersion, UUID causationId) {

        UUID bookingId = UuidGenerator.next();

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                InventoryEventContract.BOOKING_AGGREGATE_TYPE,
                eventType,
                eventVersion,
                OCCURRED_AT,
                InventoryEventContract.BOOKING_PRODUCER,
                UuidGenerator.next(),
                causationId,
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
