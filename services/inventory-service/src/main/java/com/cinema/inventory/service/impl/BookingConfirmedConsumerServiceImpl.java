package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingConfirmedPayload;
import com.cinema.inventory.event.payload.ConfirmedSeatPayload;
import com.cinema.inventory.event.serialization.BookingConfirmedPayloadReader;
import com.cinema.inventory.event.validation.BookingConfirmedMessageValidator;
import com.cinema.inventory.event.validation.BookingConfirmedPayloadValidator;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingConfirmedConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingConfirmedConsumerServiceImpl implements BookingConfirmedConsumerService {

    private final BookingConfirmedMessageValidator messageValidator;

    private final BookingConfirmedPayloadReader payloadReader;

    private final BookingConfirmedPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final ShowSeatRepository showSeatRepository;

    public BookingConfirmedConsumerServiceImpl(
            BookingConfirmedMessageValidator messageValidator,
            BookingConfirmedPayloadReader payloadReader,
            BookingConfirmedPayloadValidator payloadValidator,
            ProcessedEventRegistrationService processedEventRegistrationService,
            ShowSeatRepository showSeatRepository) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.payloadValidator = payloadValidator;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.showSeatRepository = showSeatRepository;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validate(partitionKey, message);

        BookingConfirmedPayload payload = payloadReader.read(message);

        payloadValidator.validate(partitionKey, message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CONFIRMED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        Map<String, ConfirmedSeatPayload> expectedSeats = indexExpectedSeats(payload);

        List<String> seatNumbers = expectedSeats.keySet().stream().sorted().toList();

        List<ShowSeat> showSeats =
                showSeatRepository.findAllByShowtimeIdAndSeatNumbersForUpdate(
                        payload.showtimeId(), seatNumbers);

        validateCompleteSeatSet(expectedSeats, showSeats);

        /*
         * Validate every seat before mutating any entity. This keeps the
         * aggregate operation all-or-nothing even outside a managed context.
         */
        showSeats.forEach(
                showSeat ->
                        validateShowSeat(
                                payload, expectedSeats.get(showSeat.getSeatNumber()), showSeat));

        showSeats.forEach(showSeat -> showSeat.book(payload.bookingId()));

        showSeatRepository.saveAll(showSeats);

        return Result.booked();
    }

    private Map<String, ConfirmedSeatPayload> indexExpectedSeats(BookingConfirmedPayload payload) {

        Map<String, ConfirmedSeatPayload> expectedSeats = new HashMap<>();

        for (ConfirmedSeatPayload seat : payload.seats()) {

            ConfirmedSeatPayload previous = expectedSeats.put(seat.seatNumber(), seat);

            if (previous != null) {
                throw mismatch();
            }
        }

        return expectedSeats;
    }

    private void validateCompleteSeatSet(
            Map<String, ConfirmedSeatPayload> expectedSeats, List<ShowSeat> showSeats) {

        if (showSeats == null || showSeats.size() != expectedSeats.size()) {

            throw mismatch();
        }

        boolean unexpectedSeat =
                showSeats.stream()
                        .anyMatch(
                                showSeat ->
                                        showSeat == null
                                                || !expectedSeats.containsKey(
                                                        showSeat.getSeatNumber()));

        if (unexpectedSeat) {
            throw mismatch();
        }
    }

    private void validateShowSeat(
            BookingConfirmedPayload payload, ConfirmedSeatPayload expectedSeat, ShowSeat showSeat) {

        if (expectedSeat == null) {
            throw mismatch();
        }

        if (!showSeat.isHeldBy(payload.bookingId())) {

            throw new ConflictException(InventoryErrorCode.SEAT_NOT_HELD_BY_BOOKING);
        }

        if (showSeat.getSeatType() == null
                || !showSeat.getSeatType().name().equals(expectedSeat.seatType())
                || showSeat.getPrice() == null
                || showSeat.getPrice().compareTo(expectedSeat.price()) != 0) {

            throw mismatch();
        }
    }

    private ConflictException mismatch() {

        return new ConflictException(InventoryErrorCode.BOOKING_CONFIRMATION_MISMATCH);
    }
}
