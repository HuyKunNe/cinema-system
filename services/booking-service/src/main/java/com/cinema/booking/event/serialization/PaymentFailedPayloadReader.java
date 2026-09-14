package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentFailedPayloadReader {

    PaymentFailedPayload read(OutboxEventMessage message);
}
