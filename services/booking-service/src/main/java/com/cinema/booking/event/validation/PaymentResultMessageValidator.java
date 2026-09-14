package com.cinema.booking.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentResultMessageValidator {

    void validateSucceeded(String partitionKey, OutboxEventMessage message);

    void validateFailed(String partitionKey, OutboxEventMessage message);
}
