package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingCancelledPayload;

public interface BookingCancelledPayloadValidator {

    void validate(String partitionKey, OutboxEventMessage message, BookingCancelledPayload payload);
}
