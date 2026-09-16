package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingExpiredPayload;

public interface BookingExpiredPayloadReader {

    BookingExpiredPayload read(OutboxEventMessage message);
}
