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

        List<ShowSeat> showSeats =
                showSeatRepository.findAllHeldByBookingForUpdate(
                        payload.showtimeId(), payload.bookingId());

        showSeats.forEach(showSeat -> showSeat.release(payload.bookingId()));

        showSeatRepository.saveAll(showSeats);

        return Result.released();
    }
}
