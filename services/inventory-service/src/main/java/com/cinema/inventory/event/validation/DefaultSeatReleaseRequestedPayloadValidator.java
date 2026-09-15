package com.cinema.inventory.event.validation;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultSeatReleaseRequestedPayloadValidator
        implements SeatReleaseRequestedPayloadValidator {

    @Override
    public void validate(
            String partitionKey, OutboxEventMessage message, SeatReleaseRequestedPayload payload) {

        if (message == null || payload == null) {
            throw invalidPayload();
        }

        validateIdentifiers(partitionKey, message, payload);

        validateSeatIds(payload);

        validateReason(payload.reason());

        validateRequestedAt(message.occurredAt(), payload.requestedAt());
    }

    private static void validateIdentifiers(
            String partitionKey, OutboxEventMessage message, SeatReleaseRequestedPayload payload) {

        requireUuidV7(payload.bookingId());

        requireUuidV7(payload.showtimeId());

        if (!payload.bookingId().equals(message.aggregateId())) {
            throw invalidPayload();
        }

        if (partitionKey == null || !payload.bookingId().toString().equals(partitionKey)) {

            throw new ValidationException(InventoryErrorCode.EVENT_PARTITION_KEY_INVALID);
        }
    }

    private static void validateSeatIds(SeatReleaseRequestedPayload payload) {

        if (payload.seatIds() == null || payload.seatIds().isEmpty()) {
            throw invalidPayload();
        }

        Set<UUID> uniqueSeatIds = new HashSet<>();

        for (UUID seatId : payload.seatIds()) {

            requireUuidV7(seatId);

            if (!uniqueSeatIds.add(seatId)) {
                throw invalidPayload();
            }
        }
    }

    private static void validateReason(String reason) {

        if (!InventoryEventContract.SEAT_RELEASE_REASON_PAYMENT_FAILED.equals(reason)) {

            throw invalidPayload();
        }
    }

    private static void validateRequestedAt(OffsetDateTime occurredAt, OffsetDateTime requestedAt) {

        if (occurredAt == null || requestedAt == null || !requestedAt.isEqual(occurredAt)) {

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
