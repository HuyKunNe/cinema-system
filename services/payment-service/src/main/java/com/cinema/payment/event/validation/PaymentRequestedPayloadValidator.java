package com.cinema.payment.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;

public interface PaymentRequestedPayloadValidator {

    void validate(OutboxEventMessage message, PaymentRequestedPayload payload);
}
