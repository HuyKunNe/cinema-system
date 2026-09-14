package com.cinema.booking.event.serialization;

import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.stereotype.Component;

@Component
public class JacksonPaymentResultMessageReader implements PaymentResultMessageReader {

    private final ObjectReader objectReader;

    public JacksonPaymentResultMessageReader(ObjectMapper objectMapper) {

        this.objectReader =
                objectMapper
                        .readerFor(OutboxEventMessage.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public OutboxEventMessage read(String serializedMessage) {

        if (serializedMessage == null || serializedMessage.isBlank()) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        try {
            return objectReader.readValue(serializedMessage);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID, exception);
        }
    }
}
