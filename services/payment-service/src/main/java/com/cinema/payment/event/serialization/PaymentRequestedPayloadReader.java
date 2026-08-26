package com.cinema.payment.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;

public interface PaymentRequestedPayloadReader {

    PaymentRequestedPayload read(OutboxEventMessage message);
}
