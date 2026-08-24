package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservedPayloadReader {

    SeatReservedPayload read(OutboxEventMessage message);
}
