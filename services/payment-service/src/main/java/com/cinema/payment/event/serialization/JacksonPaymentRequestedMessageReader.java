package com.cinema.payment.event.serialization;

import org.springframework.stereotype.Component;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Component
public class JacksonPaymentRequestedMessageReader implements PaymentRequestedMessageReader {

    private final ObjectReader objectReader;

    public JacksonPaymentRequestedMessageReader(ObjectMapper objectMapper) {

        this.objectReader =
                objectMapper
                        .readerFor(OutboxEventMessage.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public OutboxEventMessage read(String serializedMessage) {

        if (serializedMessage == null || serializedMessage.isBlank()) {

            throw new ValidationException(PaymentErrorCode.EVENT_MESSAGE_INVALID);
        }

        try {
            return objectReader.readValue(serializedMessage);

        } catch (JsonProcessingException exception) {
            throw new ValidationException(PaymentErrorCode.EVENT_MESSAGE_INVALID, exception);
        }
    }
}
