package com.cinema.inventory.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultBookingCancelledPayloadValidator implements BookingCancelledPayloadValidator {

    @Override
    public void validate(
            String partitionKey, OutboxEventMessage message, BookingCancelledPayload payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }

        validateIdentifiers(partitionKey, message, payload);

        validateReason(payload.reason());

        validateCancelledAt(message.occurredAt(), payload.cancelledAt());
    }

    private static void validateIdentifiers(
            String partitionKey, OutboxEventMessage message, BookingCancelledPayload payload) {

        requireUuidV7(payload.bookingId());

        requireUuidV7(payload.userId());

        requireUuidV7(payload.showtimeId());

        if (!payload.bookingId().equals(message.aggregateId())) {
            throw invalidPayload();
        }

        if (partitionKey == null || !payload.bookingId().toString().equals(partitionKey)) {

            throw new ValidationException(InventoryErrorCode.EVENT_PARTITION_KEY_INVALID);
        }
    }

    private static void validateReason(String reason) {

        if (!InventoryEventContract.BOOKING_CANCELLATION_REASON_USER_REQUESTED.equals(reason)) {

            throw invalidPayload();
        }
    }

    private static void validateCancelledAt(OffsetDateTime occurredAt, OffsetDateTime cancelledAt) {

        if (occurredAt == null || cancelledAt == null || !cancelledAt.isEqual(occurredAt)) {

            throw invalidPayload();
        }
    }

    private static void requireUuidV7(UUID value) {

        if (value == null || value.version() != 7) {
            throw invalidPayload();
        }
    }

    private static ValidationException invalidPayload() {

        return new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
    }
}
