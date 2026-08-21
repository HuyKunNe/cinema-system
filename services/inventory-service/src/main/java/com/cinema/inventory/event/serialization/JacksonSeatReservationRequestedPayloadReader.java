package com.cinema.inventory.event.serialization;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JacksonSeatReservationRequestedPayloadReader
        implements SeatReservationRequestedPayloadReader {

    private final ObjectMapper objectMapper;

    public JacksonSeatReservationRequestedPayloadReader(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public SeatReservationRequestedPayload read(OutboxEventMessage message) {

        if (message == null || message.payload() == null || message.payload().isNull()) {

            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID);
        }

        try {
            return objectMapper.treeToValue(
                    message.payload(), SeatReservationRequestedPayload.class);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(InventoryErrorCode.EVENT_PAYLOAD_INVALID, exception);
        }
    }
}
