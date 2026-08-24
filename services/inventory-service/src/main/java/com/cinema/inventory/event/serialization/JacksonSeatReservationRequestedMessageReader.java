package com.cinema.inventory.event.serialization;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JacksonSeatReservationRequestedMessageReader
        implements SeatReservationRequestedMessageReader {

    private final ObjectMapper objectMapper;

    public JacksonSeatReservationRequestedMessageReader(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventMessage read(String message) {

        if (message == null || message.isBlank()) {
            throw new ValidationException(InventoryErrorCode.EVENT_MESSAGE_INVALID);
        }

        try {
            return objectMapper.readValue(message, OutboxEventMessage.class);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(InventoryErrorCode.EVENT_MESSAGE_INVALID, exception);
        }
    }
}
