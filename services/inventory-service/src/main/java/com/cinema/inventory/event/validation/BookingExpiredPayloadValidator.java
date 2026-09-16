package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingExpiredPayload;

public interface BookingExpiredPayloadValidator {

    void validate(String partitionKey, OutboxEventMessage message, BookingExpiredPayload payload);
}
