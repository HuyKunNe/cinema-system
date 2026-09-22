package com.cinema.inventory.service.impl;

import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.payload.BookingExpiredPayload;
import com.cinema.inventory.event.serialization.BookingExpiredPayloadReader;
import com.cinema.inventory.event.validation.BookingExpiredMessageValidator;
import com.cinema.inventory.event.validation.BookingExpiredPayloadValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.service.BookingExpiredConsumerService;
import com.cinema.inventory.service.ProcessedEventRegistrationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingExpiredConsumerServiceImpl implements BookingExpiredConsumerService {

    private final BookingExpiredMessageValidator messageValidator;

    private final BookingExpiredPayloadReader payloadReader;

    private final BookingExpiredPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final ShowSeatRepository showSeatRepository;

    public BookingExpiredConsumerServiceImpl(
            BookingExpiredMessageValidator messageValidator,
            BookingExpiredPayloadReader payloadReader,
            BookingExpiredPayloadValidator payloadValidator,
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

        BookingExpiredPayload payload = payloadReader.read(message);

        payloadValidator.validate(partitionKey, message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        InventoryEventContract.BOOKING_EXPIRED_CONSUMER,
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
