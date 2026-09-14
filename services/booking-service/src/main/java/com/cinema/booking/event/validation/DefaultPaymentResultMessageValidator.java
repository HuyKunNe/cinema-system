package com.cinema.booking.event.validation;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultPaymentResultMessageValidator implements PaymentResultMessageValidator {

    @Override
    public void validateSucceeded(String partitionKey, OutboxEventMessage message) {

        validate(
                partitionKey,
                message,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION);
    }

    @Override
    public void validateFailed(String partitionKey, OutboxEventMessage message) {

        validate(
                partitionKey,
                message,
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION);
    }

    private static void validate(
            String partitionKey,
            OutboxEventMessage message,
            String expectedEventType,
            String expectedEventVersion) {

        if (message == null) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        validateEventId(message.eventId());
        validateContract(message, expectedEventType, expectedEventVersion);
        validateAggregate(message);
        validatePartitionKey(partitionKey);
        validateTracing(message);
        validateContent(message);
    }

    private static void validateEventId(UUID eventId) {

        requireUuidV7(
                eventId, BookingErrorCode.EVENT_ID_REQUIRED, BookingErrorCode.EVENT_ID_INVALID);
    }

    private static void validateContract(
            OutboxEventMessage message, String expectedEventType, String expectedEventVersion) {

        if (!expectedEventType.equals(message.eventType())) {
            throw new ValidationException(BookingErrorCode.EVENT_TYPE_INVALID);
        }

        if (!expectedEventVersion.equals(message.eventVersion())) {
            throw new ValidationException(BookingErrorCode.EVENT_VERSION_INVALID);
        }

        if (!BookingEventContract.PAYMENT_PRODUCER.equals(message.producer())) {

            throw new ValidationException(BookingErrorCode.EVENT_PRODUCER_INVALID);
        }
    }

    private static void validateAggregate(OutboxEventMessage message) {

        requireUuidV7(
                message.aggregateId(),
                BookingErrorCode.EVENT_AGGREGATE_INVALID,
                BookingErrorCode.EVENT_AGGREGATE_INVALID);

        if (!BookingEventContract.PAYMENT_AGGREGATE_TYPE.equals(message.aggregateType())) {

            throw new ValidationException(BookingErrorCode.EVENT_AGGREGATE_INVALID);
        }
    }

    private static void validatePartitionKey(String partitionKey) {

        if (partitionKey == null || partitionKey.isBlank()) {
            throw new ValidationException(BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
        }

        try {
            UUID value = UUID.fromString(partitionKey);

            if (value.version() != 7 || !value.toString().equals(partitionKey)) {

                throw new ValidationException(BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
            }

        } catch (IllegalArgumentException exception) {
            throw new ValidationException(BookingErrorCode.EVENT_PARTITION_KEY_INVALID, exception);
        }
    }

    private static void validateTracing(OutboxEventMessage message) {

        requireUuidV7(
                message.correlationId(),
                BookingErrorCode.EVENT_CORRELATION_ID_INVALID,
                BookingErrorCode.EVENT_CORRELATION_ID_INVALID);

        requireUuidV7(
                message.causationId(),
                BookingErrorCode.EVENT_CAUSATION_ID_REQUIRED,
                BookingErrorCode.EVENT_CAUSATION_ID_INVALID);
    }

    private static void validateContent(OutboxEventMessage message) {

        if (message.occurredAt() == null) {
            throw new ValidationException(BookingErrorCode.EVENT_OCCURRED_AT_REQUIRED);
        }

        if (message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
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
