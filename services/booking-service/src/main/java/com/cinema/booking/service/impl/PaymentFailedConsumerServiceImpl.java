package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.PaymentFailureReason;
import com.cinema.booking.event.SeatReleaseRequestedOutboxFactory;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.serialization.PaymentFailedPayloadReader;
import com.cinema.booking.event.validation.PaymentResultMessageValidator;
import com.cinema.booking.event.validation.PaymentResultPayloadValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.PaymentFailedConsumerService;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PaymentFailedConsumerServiceImpl implements PaymentFailedConsumerService {

    private final PaymentResultMessageValidator messageValidator;

    private final PaymentFailedPayloadReader payloadReader;

    private final PaymentResultPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final SeatReleaseRequestedOutboxFactory seatReleaseRequestedOutboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public PaymentFailedConsumerServiceImpl(
            PaymentResultMessageValidator messageValidator,
            PaymentFailedPayloadReader payloadReader,
            PaymentResultPayloadValidator payloadValidator,
            ProcessedEventRegistrationService processedEventRegistrationService,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            SeatReleaseRequestedOutboxFactory seatReleaseRequestedOutboxFactory,
            OutboxService outboxService,
            @Qualifier("systemClock") Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.payloadValidator = payloadValidator;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatReleaseRequestedOutboxFactory = seatReleaseRequestedOutboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validateFailed(partitionKey, message);

        PaymentFailedPayload payload = payloadReader.read(message);

        payloadValidator.validateFailed(partitionKey, message, payload);

        /*
         * Convert the external string to an approved stable reason
         * before inserting the processed-event marker.
         */
        PaymentFailureReason reason = PaymentFailureReason.from(payload.failureCode());

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        BookingEventContract.PAYMENT_FAILED_CONSUMER,
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

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(payload.bookingId());

        booking.failPayment(reason.name());

        Booking savedBooking = bookingRepository.save(booking);

        OffsetDateTime requestedAt = OffsetDateTime.now(clock);

        OutboxEventEntity seatReleaseEvent =
                seatReleaseRequestedOutboxFactory.create(savedBooking, seats, message, requestedAt);

        outboxService.save(seatReleaseEvent);

        return Result.paymentFailed();
    }
}
