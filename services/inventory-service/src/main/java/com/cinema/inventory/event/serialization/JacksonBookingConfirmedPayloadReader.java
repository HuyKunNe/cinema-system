package com.cinema.inventory.event.serialization;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JacksonBookingConfirmedPayloadReader implements BookingConfirmedPayloadReader {

    private final ObjectMapper objectMapper;

    public JacksonBookingConfirmedPayloadReader(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public BookingConfirmedPayload read(OutboxEventMessage message) {

        if (message == null
                || message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }

        try {
            return objectMapper.treeToValue(message.payload(), BookingConfirmedPayload.class);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID, exception);
        }
    }
}
