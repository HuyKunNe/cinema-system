package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JacksonSeatReservedPayloadReader implements SeatReservedPayloadReader {

    private final ObjectMapper objectMapper;

    public JacksonSeatReservedPayloadReader(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public SeatReservedPayload read(OutboxEventMessage message) {

        if (message == null
                || message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        try {
            return objectMapper.treeToValue(message.payload(), SeatReservedPayload.class);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID, exception);
        }
    }
}
