package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;

public interface SeatReservationRequestedPayloadReader {

    SeatReservationRequestedPayload read(OutboxEventMessage message);
}
