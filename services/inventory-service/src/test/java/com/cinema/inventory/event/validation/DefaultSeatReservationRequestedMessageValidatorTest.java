package com.cinema.inventory.event.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultSeatReservationRequestedMessageValidatorTest {

    private final DefaultSeatReservationRequestedMessageValidator validator =
            new DefaultSeatReservationRequestedMessageValidator();

    @Test
    void validMessageShouldPassValidation() {

        OutboxEventMessage message = validMessage();

        assertThatCode(() -> validator.validate(message.aggregateId().toString(), message))
                .doesNotThrowAnyException();
    }

    @Test
    void incorrectPartitionKeyShouldBeRejected() {

        assertThatThrownBy(() -> validator.validate(UUID.randomUUID().toString(), validMessage()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void unsupportedEventTypeShouldBeRejected() {

        OutboxEventMessage source = validMessage();

        OutboxEventMessage message =
                new OutboxEventMessage(
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

        assertThatThrownBy(() -> validator.validate(message.aggregateId().toString(), message))
                .isInstanceOf(ValidationException.class);
    }

    private OutboxEventMessage validMessage() {

        UUID bookingId = UuidGenerator.next();

        return new OutboxEventMessage(
                UuidGenerator.next(),
                bookingId,
                "BOOKING",
                InventoryEventContract.SEAT_RESERVATION_REQUESTED,
                InventoryEventContract.SEAT_RESERVATION_REQUESTED_VERSION,
                OffsetDateTime.now(),
                "booking-service",
                UuidGenerator.next(),
                null,
                JsonNodeFactory.instance.objectNode());
    }
}
