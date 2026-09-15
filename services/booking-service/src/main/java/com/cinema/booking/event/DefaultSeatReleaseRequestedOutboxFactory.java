package com.cinema.booking.event;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.SeatReleaseRequestedPayload;
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

@Component
public class DefaultSeatReleaseRequestedOutboxFactory implements SeatReleaseRequestedOutboxFactory {

    private final ObjectMapper objectMapper;

    public DefaultSeatReleaseRequestedOutboxFactory(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    @Override
    public OutboxEventEntity create(
            Booking booking,
            List<BookingSeat> seats,
            OutboxEventMessage sourceEvent,
            OffsetDateTime requestedAt) {

        validateInput(booking, seats, sourceEvent, requestedAt);

        List<UUID> seatIds = seats.stream().map(BookingSeat::getInventorySeatId).toList();

        SeatReleaseRequestedPayload payload =
                new SeatReleaseRequestedPayload(
                        booking.getId(),
                        booking.getShowtimeId(),
                        seatIds,
                        BookingStatus.PAYMENT_FAILED.name(),
                        requestedAt);

        return new OutboxEventEntity(
                UuidGenerator.next(),
                AggregateType.BOOKING,
                booking.getId(),
                BookingEventContract.SEAT_RELEASE_REQUESTED,
                BookingEventContract.SEAT_RELEASE_REQUESTED_VERSION,
                BookingEventContract.SEAT_RELEASE_REQUESTED,
                booking.getId().toString(),
                requestedAt,
                sourceEvent.correlationId(),
                sourceEvent.eventId(),
                serialize(payload),
                requestedAt);
    }

    private static void validateInput(
            Booking booking,
            List<BookingSeat> seats,
            OutboxEventMessage sourceEvent,
            OffsetDateTime requestedAt) {

        if (booking == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }

        if (booking.getStatus() != BookingStatus.PAYMENT_FAILED) {
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_PAYMENT_FAILED);
        }

        if (sourceEvent == null) {
            throw new ValidationException(BookingErrorCode.EVENT_MESSAGE_INVALID);
        }

        if (requestedAt == null) {
            throw new ValidationException(BookingErrorCode.CURRENT_TIME_REQUIRED);
        }

        if (seats == null || seats.isEmpty()) {
            throw new ValidationException(BookingErrorCode.SEAT_NUMBERS_REQUIRED);
        }

        boolean invalidSeat =
                seats.stream()
                        .anyMatch(
                                seat ->
                                        seat == null
                                                || !seat.hasCompletedSnapshot()
                                                || seat.getInventorySeatId().version() != 7
                                                || !booking.getId().equals(seat.getBookingId())
                                                || !booking.getShowtimeId()
                                                        .equals(seat.getShowtimeId()));

        if (invalidSeat) {
            throw new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
        }
    }

    private String serialize(SeatReleaseRequestedPayload payload) {

        try {
            return objectMapper.writeValueAsString(payload);

        } catch (JsonProcessingException exception) {
            throw new InternalServerException(
                    BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED, exception);
        }
    }
}
