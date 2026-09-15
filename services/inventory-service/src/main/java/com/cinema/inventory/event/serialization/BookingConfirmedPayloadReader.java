package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;

public interface BookingConfirmedPayloadReader {

    BookingConfirmedPayload read(OutboxEventMessage message);
}
