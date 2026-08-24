package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.event.payload.BookingExpiredPayload;
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
public class DefaultBookingExpiredOutboxFactory implements BookingExpiredOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultBookingExpiredOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(Booking booking, OffsetDateTime expiredAt) {

        BookingExpiredPayload payload =
                new BookingExpiredPayload(
                        booking.getId(), booking.getUserId(), booking.getShowtimeId(), expiredAt);

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.BOOKING_EXPIRED,
                BookingEventContract.BOOKING_EXPIRED_VERSION,
                BookingEventContract.BOOKING_EXPIRED,
                booking.getId().toString(),
                expiredAt,
                correlationId,
                null,
                serialize(payload),
                expiredAt);
    }

    private String serialize(BookingExpiredPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
