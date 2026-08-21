package com.cinema.inventory.event.validation;

import com.cinema.common.outbox.model.OutboxEventMessage;

public interface SeatReservationRequestedMessageValidator {

    void validate(String partitionKey, OutboxEventMessage message);
}
