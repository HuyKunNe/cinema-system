package com.cinema.booking.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservedMessageValidator {

    void validate(String partitionKey, OutboxEventMessage message);
}
