package com.cinema.inventory.event.serialization;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;

public interface SeatReleaseRequestedPayloadReader {

    SeatReleaseRequestedPayload read(OutboxEventMessage message);
}
