package com.cinema.payment.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentRequestedMessageValidator {

    void validate(String partitionKey, OutboxEventMessage message);
}
