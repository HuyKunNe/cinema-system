package com.cinema.booking.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservationRejectedMessageReader {

    OutboxEventMessage read(String serializedMessage);
}
