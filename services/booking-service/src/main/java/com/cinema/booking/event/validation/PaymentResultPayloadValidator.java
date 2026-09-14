package com.cinema.booking.event.validation;

import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentResultPayloadValidator {

    void validateSucceeded(
            String partitionKey, OutboxEventMessage message, PaymentSucceededPayload payload);

    void validateFailed(
            String partitionKey, OutboxEventMessage message, PaymentFailedPayload payload);
}
