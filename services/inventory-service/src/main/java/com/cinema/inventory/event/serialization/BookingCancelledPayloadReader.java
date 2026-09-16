package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingCancelledPayload;

public interface BookingCancelledPayloadReader {

    BookingCancelledPayload read(OutboxEventMessage message);
}
