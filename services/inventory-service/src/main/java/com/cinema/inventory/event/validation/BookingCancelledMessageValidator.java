package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface BookingCancelledMessageValidator {

    void validate(String partitionKey, OutboxEventMessage message);
}
