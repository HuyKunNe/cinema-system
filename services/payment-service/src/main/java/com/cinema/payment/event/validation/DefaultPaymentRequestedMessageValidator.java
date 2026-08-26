package com.cinema.payment.event.validation;

import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.PaymentEventContract;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DefaultPaymentRequestedMessageValidator implements PaymentRequestedMessageValidator {

    @Override
    public void validate(String partitionKey, OutboxEventMessage message) {

        if (message == null) {
            throw new ValidationException(PaymentErrorCode.EVENT_MESSAGE_INVALID);
        }

        validateEventId(message.eventId());
        validateContract(message);
        validateAggregate(partitionKey, message);
        validateTracing(message);
        validateContent(message);
    }

    private static void validateContract(OutboxEventMessage message) {

        if (!PaymentEventContract.PAYMENT_REQUESTED.equals(message.eventType())) {

            throw new ValidationException(PaymentErrorCode.EVENT_TYPE_INVALID);
        }

        if (!PaymentEventContract.PAYMENT_REQUESTED_VERSION.equals(message.eventVersion())) {

            throw new ValidationException(PaymentErrorCode.EVENT_VERSION_INVALID);
        }

        if (!PaymentEventContract.BOOKING_PRODUCER.equals(message.producer())) {

            throw new ValidationException(PaymentErrorCode.EVENT_PRODUCER_INVALID);
        }
    }

    private static void validateAggregate(String partitionKey, OutboxEventMessage message) {

        requireUuidV7(
                message.aggregateId(),
                PaymentErrorCode.EVENT_AGGREGATE_INVALID,
                PaymentErrorCode.EVENT_AGGREGATE_INVALID);

        if (!PaymentEventContract.BOOKING_AGGREGATE_TYPE.equals(message.aggregateType())) {

            throw new ValidationException(PaymentErrorCode.EVENT_AGGREGATE_INVALID);
        }

        if (partitionKey == null || !message.aggregateId().toString().equals(partitionKey)) {

            throw new ValidationException(PaymentErrorCode.EVENT_PARTITION_KEY_INVALID);
        }
    }

    private static void validateTracing(OutboxEventMessage message) {

        requireUuidV7(
                message.correlationId(),
                PaymentErrorCode.EVENT_CORRELATION_ID_INVALID,
                PaymentErrorCode.EVENT_CORRELATION_ID_INVALID);

        requireUuidV7(
                message.causationId(),
                PaymentErrorCode.EVENT_CAUSATION_ID_REQUIRED,
                PaymentErrorCode.EVENT_CAUSATION_ID_INVALID);
    }

    private static void validateEventId(UUID eventId) {

        requireUuidV7(
                eventId, PaymentErrorCode.EVENT_ID_REQUIRED, PaymentErrorCode.EVENT_ID_INVALID);
    }

    private static void validateContent(OutboxEventMessage message) {

        if (message.occurredAt() == null) {
            throw new ValidationException(PaymentErrorCode.EVENT_OCCURRED_AT_REQUIRED);
        }

        if (message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_INVALID);
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
