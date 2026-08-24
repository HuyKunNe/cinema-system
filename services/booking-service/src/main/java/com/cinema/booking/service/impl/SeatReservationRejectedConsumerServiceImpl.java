package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.SeatReservationRejectionReason;
import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.event.serialization.SeatReservationRejectedPayloadReader;
import com.cinema.booking.event.validation.SeatReservationRejectedMessageValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatReservationRejectedConsumerServiceImpl
        implements SeatReservationRejectedConsumerService {

    private final SeatReservationRejectedMessageValidator messageValidator;

    private final SeatReservationRejectedPayloadReader payloadReader;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final BookingRepository bookingRepository;

    public SeatReservationRejectedConsumerServiceImpl(
            SeatReservationRejectedMessageValidator messageValidator,
            SeatReservationRejectedPayloadReader payloadReader,
            ProcessedEventRegistrationService processedEventRegistrationService,
            BookingRepository bookingRepository) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        SeatReservationRejectedPayload payload = payloadReader.read(message);

        SeatReservationRejectionReason reason = validatePayload(message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        BookingEventContract.SEAT_RESERVATION_REJECTED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        Booking booking =
                bookingRepository
                        .findByIdForUpdate(payload.bookingId())
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));

        validateBookingMatch(booking, payload);

        booking.reject(reason.name());

        bookingRepository.save(booking);

        return Result.rejected();
    }

    private SeatReservationRejectionReason validatePayload(
            OutboxEventMessage message, SeatReservationRejectedPayload payload) {

        if (payload == null
                || payload.bookingId() == null
                || payload.showtimeId() == null
                || payload.reasonCode() == null
                || payload.reasonCode().isBlank()
                || payload.message() == null
                || payload.message().isBlank()
                || payload.rejectedAt() == null) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        requireUuidV7(payload.bookingId());

        requireUuidV7(payload.showtimeId());

        if (!payload.bookingId().equals(message.aggregateId())) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        validateUnavailableSeats(payload.unavailableSeats());

        return SeatReservationRejectionReason.from(payload.reasonCode());
    }

    private void validateBookingMatch(Booking booking, SeatReservationRejectedPayload payload) {

        if (!booking.getShowtimeId().equals(payload.showtimeId())) {

            throw new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
        }
    }

    private void validateUnavailableSeats(List<String> unavailableSeats) {

        if (unavailableSeats == null) {
            return;
        }

        Set<String> normalizedSeats = new HashSet<>();

        for (String seatNumber : unavailableSeats) {

            if (seatNumber == null || seatNumber.isBlank()) {

                throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
            }

            String normalized = seatNumber.trim().toUpperCase(Locale.ROOT);

            if (!normalizedSeats.add(normalized)) {

                throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
            }
        }
    }

    private void requireUuidV7(UUID value) {

        if (value.version() != 7) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }
}
