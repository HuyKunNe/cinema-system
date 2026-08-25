package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.PaymentRequestedOutboxFactory;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.event.serialization.SeatReservedPayloadReader;
import com.cinema.booking.event.validation.SeatReservedMessageValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatReservedConsumerServiceImpl implements SeatReservedConsumerService {

    private final SeatReservedMessageValidator messageValidator;

    private final SeatReservedPayloadReader payloadReader;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final PaymentRequestedOutboxFactory paymentRequestedOutboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public SeatReservedConsumerServiceImpl(
            SeatReservedMessageValidator messageValidator,
            SeatReservedPayloadReader payloadReader,
            ProcessedEventRegistrationService processedEventRegistrationService,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRequestedOutboxFactory paymentRequestedOutboxFactory,
            OutboxService outboxService,
            @Qualifier("systemClock") Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.paymentRequestedOutboxFactory = paymentRequestedOutboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        SeatReservedPayload payload = payloadReader.read(message);

        validatePayloadIdentifiers(message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        BookingEventContract.SEAT_RESERVED_CONSUMER,
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

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(payload.bookingId());

        Map<String, ReservedSeatPayload> reservedSeats = validateAndIndexReservedSeats(payload);

        validateRequestedSeats(bookingSeats, reservedSeats);

        BigDecimal calculatedTotal = completeSeatSnapshots(bookingSeats, reservedSeats);

        validateTotal(payload.totalAmount(), calculatedTotal);

        booking.reserve(payload.totalAmount(), payload.currency());

        bookingSeatRepository.saveAll(bookingSeats);

        Booking savedBooking = bookingRepository.save(booking);

        OffsetDateTime requestedAt = OffsetDateTime.now(clock);

        OutboxEventEntity paymentRequestedEvent =
                paymentRequestedOutboxFactory.create(savedBooking, message, requestedAt);

        outboxService.save(paymentRequestedEvent);

        return Result.reserved();
    }

    private void validatePayloadIdentifiers(
            OutboxEventMessage message, SeatReservedPayload payload) {

        if (payload == null
                || payload.bookingId() == null
                || payload.showtimeId() == null
                || payload.seats() == null
                || payload.seats().isEmpty()
                || payload.totalAmount() == null
                || payload.currency() == null
                || payload.currency().isBlank()
                || payload.heldAt() == null
                || payload.holdExpiresAt() == null) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        if (!payload.bookingId().equals(message.aggregateId())) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        requireUuidV7(payload.bookingId());
        requireUuidV7(payload.showtimeId());

        if (!payload.holdExpiresAt().isAfter(payload.heldAt())) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }

    private void validateBookingMatch(Booking booking, SeatReservedPayload payload) {

        if (!booking.getShowtimeId().equals(payload.showtimeId())) {

            throw mismatch();
        }

        if (!booking.getExpiresAt().equals(payload.holdExpiresAt())) {

            throw mismatch();
        }
    }

    private Map<String, ReservedSeatPayload> validateAndIndexReservedSeats(
            SeatReservedPayload payload) {

        Map<String, ReservedSeatPayload> result = new HashMap<>();

        Set<UUID> inventorySeatIds = new HashSet<>();

        for (ReservedSeatPayload seat : payload.seats()) {

            validateReservedSeat(seat);

            String normalizedSeatNumber = normalizeSeatNumber(seat.seatNumber());

            if (result.putIfAbsent(normalizedSeatNumber, seat) != null) {

                throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
            }

            if (!inventorySeatIds.add(seat.inventorySeatId())) {

                throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
            }
        }

        return result;
    }

    private void validateReservedSeat(ReservedSeatPayload seat) {

        if (seat == null
                || seat.inventorySeatId() == null
                || seat.seatNumber() == null
                || seat.seatNumber().isBlank()
                || seat.seatType() == null
                || seat.seatType().isBlank()
                || seat.price() == null
                || seat.price().signum() < 0) {

            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        requireUuidV7(seat.inventorySeatId());
    }

    private void validateRequestedSeats(
            List<BookingSeat> bookingSeats, Map<String, ReservedSeatPayload> reservedSeats) {

        if (bookingSeats.isEmpty() || bookingSeats.size() != reservedSeats.size()) {

            throw mismatch();
        }

        Set<String> requestedSeatNumbers =
                bookingSeats.stream()
                        .map(BookingSeat::getSeatNumber)
                        .map(this::normalizeSeatNumber)
                        .collect(java.util.stream.Collectors.toSet());

        if (!requestedSeatNumbers.equals(reservedSeats.keySet())) {

            throw mismatch();
        }
    }

    private BigDecimal completeSeatSnapshots(
            List<BookingSeat> bookingSeats, Map<String, ReservedSeatPayload> reservedSeats) {

        BigDecimal calculatedTotal = BigDecimal.ZERO;

        for (BookingSeat bookingSeat : bookingSeats) {

            String seatNumber = normalizeSeatNumber(bookingSeat.getSeatNumber());

            ReservedSeatPayload reservedSeat = reservedSeats.get(seatNumber);

            if (reservedSeat == null) {
                throw mismatch();
            }

            bookingSeat.completeSnapshot(
                    reservedSeat.inventorySeatId(), reservedSeat.seatType(), reservedSeat.price());

            calculatedTotal = calculatedTotal.add(reservedSeat.price());
        }

        return calculatedTotal;
    }

    private void validateTotal(BigDecimal eventTotal, BigDecimal calculatedTotal) {

        if (eventTotal.signum() < 0) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }

        if (eventTotal.compareTo(calculatedTotal) != 0) {

            throw mismatch();
        }
    }

    private void requireUuidV7(UUID value) {

        if (value.version() != 7) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
    }

    private String normalizeSeatNumber(String seatNumber) {

        return seatNumber.trim().toUpperCase(Locale.ROOT);
    }

    private ConflictException mismatch() {

        return new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
    }
}
