package com.cinema.inventory.event.validation;

import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultBookingCancelledMessageValidator implements BookingCancelledMessageValidator {

    @Override
    public void validate(String partitionKey, OutboxEventMessage message) {

        if (message == null) {
            throw new ValidationException(InventoryErrorCode.EVENT_MESSAGE_INVALID);
        }

        requireUuidV7(
                message.eventId(),
                InventoryErrorCode.EVENT_ID_REQUIRED,
                InventoryErrorCode.EVENT_ID_INVALID);

        if (!InventoryEventContract.BOOKING_CANCELLED.equals(message.eventType())) {

            throw new ValidationException(InventoryErrorCode.EVENT_TYPE_INVALID);
        }

        if (!InventoryEventContract.BOOKING_CANCELLED_VERSION.equals(message.eventVersion())) {

            throw new ValidationException(InventoryErrorCode.EVENT_VERSION_INVALID);
        }

        validateBookingEnvelope(partitionKey, message);
    }

    private static void validateBookingEnvelope(String partitionKey, OutboxEventMessage message) {

        if (!InventoryEventContract.BOOKING_PRODUCER.equals(message.producer())) {

            throw new ValidationException(InventoryErrorCode.EVENT_PRODUCER_INVALID);
        }

        if (!InventoryEventContract.BOOKING_AGGREGATE_TYPE.equals(message.aggregateType())) {

            throw new ValidationException(InventoryErrorCode.EVENT_AGGREGATE_INVALID);
        }

        requireUuidV7(
                message.aggregateId(),
                InventoryErrorCode.EVENT_AGGREGATE_INVALID,
                InventoryErrorCode.EVENT_AGGREGATE_INVALID);

        if (partitionKey == null || !message.aggregateId().toString().equals(partitionKey)) {

            throw new ValidationException(InventoryErrorCode.EVENT_PARTITION_KEY_INVALID);
        }

        requireUuidV7(
                message.correlationId(),
                InventoryErrorCode.EVENT_CORRELATION_ID_INVALID,
                InventoryErrorCode.EVENT_CORRELATION_ID_INVALID);

        /*
         * Booking lifecycle events currently originate directly from
         * Booking-owned actions and therefore may have no causation ID.
         */
        if (message.causationId() != null && message.causationId().version() != 7) {

            throw new ValidationException(InventoryErrorCode.EVENT_CAUSATION_ID_INVALID);
        }

        if (message.occurredAt() == null) {
            throw new ValidationException(InventoryErrorCode.EVENT_OCCURRED_AT_REQUIRED);
        }

        if (message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }

    private static void requireUuidV7(UUID value, ErrorCode missingError, ErrorCode invalidError) {

        if (value == null) {
            throw new ValidationException(missingError);
        }

        if (value.version() != 7) {
            throw new ValidationException(invalidError);
        }
    }
}
