package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.BookingConfirmedPayload;
import com.cinema.booking.event.payload.ConfirmedSeatPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class DefaultBookingConfirmedOutboxFactory implements BookingConfirmedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultBookingConfirmedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            Booking booking,
            List<BookingSeat> seats,
            UUID paymentId,
            OutboxEventMessage sourceEvent) {

        validateInput(booking, seats, paymentId, sourceEvent);

        OffsetDateTime confirmedAt = booking.getConfirmedAt();

        List<ConfirmedSeatPayload> seatPayloads =
                seats.stream()
                        .map(
                                seat ->
                                        new ConfirmedSeatPayload(
                                                seat.getSeatNumber(),
                                                seat.getSeatType(),
                                                seat.getPrice()))
                        .toList();

        BookingConfirmedPayload payload =
                new BookingConfirmedPayload(
                        booking.getId(),
                        booking.getUserId(),
                        booking.getShowtimeId(),
                        paymentId,
                        seatPayloads,
                        booking.getTotalAmount(),
                        booking.getCurrency(),
                        confirmedAt);

        UUID eventId = UuidGenerator.next();

        return new OutboxEventEntity(
                eventId,
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.BOOKING_CONFIRMED,
                BookingEventContract.BOOKING_CONFIRMED_VERSION,
                BookingEventContract.BOOKING_CONFIRMED,
                booking.getId().toString(),
                confirmedAt,
                sourceEvent.correlationId(),
                sourceEvent.eventId(),
                serialize(payload),
                confirmedAt);
    }

    private static void validateInput(
            Booking booking,
            List<BookingSeat> seats,
            UUID paymentId,
            OutboxEventMessage sourceEvent) {

        if (booking == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getConfirmedAt() == null) {

            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        }

        if (paymentId == null) {
            throw new ValidationException(BookingErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (sourceEvent == null) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        if (seats == null || seats.isEmpty()) {
            throw new ValidationException(BookingErrorCode.SEAT_NUMBERS_REQUIRED);
        }

        boolean inconsistentSeat =
                seats.stream()
                        .anyMatch(
                                seat ->
                                        seat == null
                                                || !seat.hasCompletedSnapshot()
                                                || !booking.getId().equals(seat.getBookingId())
                                                || !booking.getShowtimeId()
                                                        .equals(seat.getShowtimeId()));

        if (inconsistentSeat) {
            throw new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
        }
    }

    private String serialize(BookingConfirmedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
