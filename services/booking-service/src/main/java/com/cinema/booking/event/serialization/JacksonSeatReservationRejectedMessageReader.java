package com.cinema.booking.event.serialization;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JacksonSeatReservationRejectedMessageReader
        implements SeatReservationRejectedMessageReader {

    private final ObjectMapper objectMapper;

    public JacksonSeatReservationRejectedMessageReader(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventMessage read(String serializedMessage) {

        if (serializedMessage == null || serializedMessage.isBlank()) {

            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        try {
            return objectMapper.readValue(serializedMessage, OutboxEventMessage.class);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID, exception);
        }
    }
}
