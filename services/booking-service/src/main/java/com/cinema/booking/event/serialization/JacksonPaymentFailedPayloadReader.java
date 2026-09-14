package com.cinema.booking.event.serialization;

import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JacksonPaymentFailedPayloadReader implements PaymentFailedPayloadReader {

    private final ObjectReader objectReader;

    public JacksonPaymentFailedPayloadReader(ObjectMapper objectMapper) {

        this.objectReader =
                objectMapper
                        .readerFor(PaymentFailedPayload.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public PaymentFailedPayload read(OutboxEventMessage message) {

        validatePayloadNode(message);

        try {
            return objectReader.readValue(message.payload());

        } catch (IOException exception) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID, exception);
        }
    }

    private static void validatePayloadNode(OutboxEventMessage message) {

        if (message == null
                || message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }
}
