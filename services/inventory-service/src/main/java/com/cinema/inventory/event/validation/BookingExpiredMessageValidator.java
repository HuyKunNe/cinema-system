package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface BookingExpiredMessageValidator {

    void validate(String partitionKey, OutboxEventMessage message);
}
