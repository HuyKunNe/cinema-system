package com.cinema.payment.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentRequestedMessageReader {

    OutboxEventMessage read(String serializedMessage);
}
