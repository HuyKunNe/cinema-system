package com.cinema.booking.event.validation;

import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultSeatReservedMessageValidator implements SeatReservedMessageValidator {

    private static final String EXPECTED_AGGREGATE_TYPE = "BOOKING";

    @Override
    public void validate(String partitionKey, OutboxEventMessage message) {

        if (message == null) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        requireUuidV7(
                message.eventId(),
                BookingErrorCode.EVENT_ID_REQUIRED,
                BookingErrorCode.EVENT_ID_INVALID);

        if (!BookingEventContract.SEAT_RESERVED.equals(message.eventType())) {

            throw new ValidationException(BookingErrorCode.EVENT_TYPE_INVALID);
        }

        if (!BookingEventContract.SEAT_RESERVED_VERSION.equals(message.eventVersion())) {

            throw new ValidationException(BookingErrorCode.EVENT_VERSION_INVALID);
        }

        if (!BookingEventContract.INVENTORY_PRODUCER.equals(message.producer())) {

            throw new ValidationException(BookingErrorCode.EVENT_PRODUCER_INVALID);
        }

        requireUuidV7(
                message.aggregateId(),
                BookingErrorCode.EVENT_AGGREGATE_INVALID,
                BookingErrorCode.EVENT_AGGREGATE_INVALID);

        if (!EXPECTED_AGGREGATE_TYPE.equals(message.aggregateType())) {

            throw new ValidationException(BookingErrorCode.EVENT_AGGREGATE_INVALID);
        }

        if (partitionKey == null || !message.aggregateId().toString().equals(partitionKey)) {

            throw new ValidationException(BookingErrorCode.EVENT_PARTITION_KEY_INVALID);
        }

        requireUuidV7(
                message.correlationId(),
                BookingErrorCode.EVENT_CORRELATION_ID_INVALID,
                BookingErrorCode.EVENT_CORRELATION_ID_INVALID);

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
