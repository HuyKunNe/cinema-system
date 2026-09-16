package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface BookingExpiredMessageReader {

    OutboxEventMessage read(String message);
}
