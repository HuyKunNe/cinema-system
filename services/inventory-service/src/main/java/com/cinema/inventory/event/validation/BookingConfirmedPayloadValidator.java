package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;

public interface BookingConfirmedPayloadValidator {

    void validate(String partitionKey, OutboxEventMessage message, BookingConfirmedPayload payload);
}
