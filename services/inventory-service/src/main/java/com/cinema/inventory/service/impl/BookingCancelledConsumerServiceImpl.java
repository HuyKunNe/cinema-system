package com.cinema.inventory.service.impl;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingCancelledPayload;
import com.cinema.inventory.event.serialization.BookingCancelledPayloadReader;
import com.cinema.inventory.event.validation.BookingCancelledMessageValidator;
import com.cinema.inventory.event.validation.BookingCancelledPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingCancelledConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingCancelledConsumerServiceImpl implements BookingCancelledConsumerService {

    private final BookingCancelledMessageValidator messageValidator;

    private final BookingCancelledPayloadReader payloadReader;

    private final BookingCancelledPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final ShowSeatRepository showSeatRepository;

    public BookingCancelledConsumerServiceImpl(
            BookingCancelledMessageValidator messageValidator,
            BookingCancelledPayloadReader payloadReader,
            BookingCancelledPayloadValidator payloadValidator,
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

        BookingCancelledPayload payload = payloadReader.read(message);

        payloadValidator.validate(partitionKey, message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_CANCELLED_CONSUMER,
                        message.eventType(),
                        message.eventVersion());

        if (!registered) {
            return Result.alreadyProcessed();
        }

        List<UUID> showSeatIds =
                showSeatRepository.findIdsByShowtimeIdAndBookingId(
                        payload.showtimeId(), payload.bookingId());

        if (showSeatIds.isEmpty()) {
            return Result.released();
        }

        List<ShowSeat> lockedShowSeats =
                showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(
                        payload.showtimeId(), showSeatIds.stream().sorted().toList());

        List<ShowSeat> releasableShowSeats =
                lockedShowSeats.stream()
                        .filter(showSeat -> showSeat.isHeldBy(payload.bookingId()))
                        .toList();

        releasableShowSeats.forEach(showSeat -> showSeat.release(payload.bookingId()));

        showSeatRepository.saveAll(releasableShowSeats);

        return Result.released();
    }
}
