package com.cinema.payment.event.serialization;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.payment.event.payload.PaymentRequestedPayload;
import com.cinema.payment.exception.PaymentErrorCode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JacksonPaymentRequestedPayloadReader implements PaymentRequestedPayloadReader {

    private final ObjectReader objectReader;

    public JacksonPaymentRequestedPayloadReader(ObjectMapper objectMapper) {

        this.objectReader =
                objectMapper
                        .readerFor(PaymentRequestedPayload.class)
                        .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public PaymentRequestedPayload read(OutboxEventMessage message) {

        if (message == null
                || message.payload() == null
                || message.payload().isNull()
                || !message.payload().isObject()) {

            throw new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_INVALID);
        }

        try {
            return objectReader.readValue(message.payload());

        } catch (IOException exception) {
            throw new ValidationException(PaymentErrorCode.EVENT_PAYLOAD_INVALID, exception);
        }
    }
}
