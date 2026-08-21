package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.payload.RequestedSeatPayload;
import com.cinema.booking.event.payload.SeatReservationRequestedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultSeatReservationRequestedOutboxFactory
        implements SeatReservationRequestedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultSeatReservationRequestedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            Booking booking, List<BookingSeat> bookingSeats, OffsetDateTime occurredAt) {

        SeatReservationRequestedPayload payload =
                new SeatReservationRequestedPayload(
                        booking.getId(),
                        booking.getUserId(),
                        booking.getShowtimeId(),
                        requestedSeats(bookingSeats),
                        occurredAt,
                        booking.getExpiresAt());

        UUID eventId = UuidGenerator.next();

        UUID correlationId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.SEAT_RESERVATION_REQUESTED,
                BookingEventContract.SEAT_RESERVATION_REQUESTED_VERSION,
                BookingEventContract.SEAT_RESERVATION_REQUESTED,
                booking.getId().toString(),
                occurredAt,
                correlationId,
                null,
                serialize(payload),
                occurredAt);
    }

    private List<RequestedSeatPayload> requestedSeats(List<BookingSeat> bookingSeats) {

        return bookingSeats.stream()
                .sorted(Comparator.comparing(BookingSeat::getSeatNumber))
                .map(bookingSeat -> new RequestedSeatPayload(bookingSeat.getSeatNumber()))
                .toList();
    }

    private String serialize(SeatReservationRequestedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
