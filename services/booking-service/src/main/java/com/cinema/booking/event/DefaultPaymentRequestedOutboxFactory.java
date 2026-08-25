package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.event.payload.PaymentRequestedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultPaymentRequestedOutboxFactory implements PaymentRequestedOutboxFactory {

    private static final int INITIAL_PAYMENT_ATTEMPT = 1;

    private final ObjectMapper objectMapper;

    public DefaultPaymentRequestedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            Booking booking, OutboxEventMessage sourceEvent, OffsetDateTime requestedAt) {

        PaymentRequestedPayload payload =
                new PaymentRequestedPayload(
                        booking.getId(),
                        booking.getUserId(),
                        booking.getTotalAmount(),
                        booking.getCurrency(),
                        INITIAL_PAYMENT_ATTEMPT,
                        booking.getExpiresAt(),
                        requestedAt);

        UUID eventId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.PAYMENT_REQUESTED,
                BookingEventContract.PAYMENT_REQUESTED_VERSION,
                BookingEventContract.PAYMENT_REQUESTED,
                booking.getId().toString(),
                requestedAt,
                sourceEvent.correlationId(),
                sourceEvent.eventId(),
                serialize(payload),
                requestedAt);
    }

    private String serialize(PaymentRequestedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
