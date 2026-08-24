package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.booking.event.payload.BookingCancelledPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class DefaultBookingCancelledOutboxFactory implements BookingCancelledOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultBookingCancelledOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(Booking booking, BookingCancellationReason reason) {

        OffsetDateTime cancelledAt = booking.getCancelledAt();

        BookingCancelledPayload payload =
                new BookingCancelledPayload(
                        booking.getId(),
                        booking.getUserId(),
                        booking.getShowtimeId(),
                        reason.name(),
                        cancelledAt);

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.BOOKING_CANCELLED,
                BookingEventContract.BOOKING_CANCELLED_VERSION,
                BookingEventContract.BOOKING_CANCELLED,
                booking.getId().toString(),
                cancelledAt,
                correlationId,
                null,
                serialize(payload),
                cancelledAt);
    }

    private String serialize(BookingCancelledPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
