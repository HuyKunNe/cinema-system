package com.cinema.inventory.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultBookingExpiredPayloadValidator implements BookingExpiredPayloadValidator {

    @Override
    public void validate(
            String partitionKey, OutboxEventMessage message, BookingExpiredPayload payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }

        validateIdentifiers(partitionKey, message, payload);

        validateExpiredAt(message.occurredAt(), payload.expiredAt());
    }

    private static void validateIdentifiers(
            String partitionKey, OutboxEventMessage message, BookingExpiredPayload payload) {

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

    private static void validateExpiredAt(OffsetDateTime occurredAt, OffsetDateTime expiredAt) {

        if (occurredAt == null || expiredAt == null || !expiredAt.isEqual(occurredAt)) {

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
