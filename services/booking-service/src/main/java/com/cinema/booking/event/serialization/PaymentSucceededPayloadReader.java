package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentSucceededPayloadReader {

    PaymentSucceededPayload read(OutboxEventMessage message);
}
