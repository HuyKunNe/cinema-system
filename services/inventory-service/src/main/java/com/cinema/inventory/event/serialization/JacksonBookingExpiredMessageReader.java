package com.cinema.inventory.event.serialization;

import org.springframework.stereotype.Component;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JacksonBookingExpiredMessageReader implements BookingExpiredMessageReader {

    private final ObjectMapper objectMapper;

    public JacksonBookingExpiredMessageReader(ObjectMapper objectMapper) {

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
