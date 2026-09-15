package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.SeatReleaseRequestedPayload;

public interface SeatReleaseRequestedPayloadValidator {

    void validate(
            String partitionKey, OutboxEventMessage message, SeatReleaseRequestedPayload payload);
}
