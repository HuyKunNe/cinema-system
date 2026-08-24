package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservationRejectedPayloadReader {

    SeatReservationRejectedPayload read(OutboxEventMessage message);
}
