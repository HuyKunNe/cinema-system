package com.cinema.inventory.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultSeatReservationRequestedMessageValidator
        implements SeatReservationRequestedMessageValidator {

    private static final String EXPECTED_AGGREGATE_TYPE = "BOOKING";

    private static final String EXPECTED_PRODUCER = "booking-service";

    @Override
    public void validate(String partitionKey, OutboxEventMessage message) {

        if (message == null) {
            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }

        requireUuidV7(
                message.eventId(),
                InventoryErrorCode.EVENT_ID_REQUIRED,
                InventoryErrorCode.EVENT_ID_INVALID);

        if (!InventoryEventContract.SEAT_RESERVATION_REQUESTED.equals(message.eventType())) {

            throw new ValidationException(InventoryErrorCode.EVENT_TYPE_INVALID);
        }

        if (!InventoryEventContract.SEAT_RESERVATION_REQUESTED_VERSION.equals(
                message.eventVersion())) {

            throw new ValidationException(InventoryErrorCode.EVENT_VERSION_INVALID);
        }

        if (!EXPECTED_PRODUCER.equals(message.producer())) {
            throw new ValidationException(InventoryErrorCode.EVENT_PRODUCER_INVALID);
        }

        if (!EXPECTED_AGGREGATE_TYPE.equals(message.aggregateType())
                || message.aggregateId() == null) {

            throw new ValidationException(InventoryErrorCode.EVENT_AGGREGATE_INVALID);
        }

        if (partitionKey == null || !message.aggregateId().toString().equals(partitionKey)) {

            throw new ValidationException(InventoryErrorCode.EVENT_PARTITION_KEY_INVALID);
        }

        requireUuidV7(
                message.correlationId(),
                InventoryErrorCode.EVENT_CORRELATION_ID_INVALID,
                InventoryErrorCode.EVENT_CORRELATION_ID_INVALID);

        if (message.occurredAt() == null) {
            throw new ValidationException(InventoryErrorCode.EVENT_OCCURRED_AT_REQUIRED);
        }

        if (message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }

    private void requireUuidV7(
            UUID value,
            com.cinema.common.exception.code.ErrorCode missingError,
            com.cinema.common.exception.code.ErrorCode invalidError) {

        if (value == null) {
            throw new ValidationException(missingError);
        }

        if (value.version() != 7) {
            throw new ValidationException(invalidError);
        }
    }
}
