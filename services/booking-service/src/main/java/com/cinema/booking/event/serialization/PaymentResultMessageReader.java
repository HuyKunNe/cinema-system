package com.cinema.booking.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface PaymentResultMessageReader {

    OutboxEventMessage read(String serializedMessage);
}
