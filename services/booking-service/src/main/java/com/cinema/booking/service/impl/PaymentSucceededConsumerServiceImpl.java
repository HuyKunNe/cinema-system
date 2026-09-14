package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.BookingConfirmedOutboxFactory;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.event.serialization.PaymentSucceededPayloadReader;
import com.cinema.booking.event.validation.PaymentResultMessageValidator;
import com.cinema.booking.event.validation.PaymentResultPayloadValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.PaymentSucceededConsumerService;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PaymentSucceededConsumerServiceImpl implements PaymentSucceededConsumerService {

    private final PaymentResultMessageValidator messageValidator;

    private final PaymentSucceededPayloadReader payloadReader;

    private final PaymentResultPayloadValidator payloadValidator;

    private final ProcessedEventRegistrationService processedEventRegistrationService;

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingConfirmedOutboxFactory bookingConfirmedOutboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public PaymentSucceededConsumerServiceImpl(
            PaymentResultMessageValidator messageValidator,
            PaymentSucceededPayloadReader payloadReader,
            PaymentResultPayloadValidator payloadValidator,
            ProcessedEventRegistrationService processedEventRegistrationService,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingConfirmedOutboxFactory bookingConfirmedOutboxFactory,
            OutboxService outboxService,
            @Qualifier("systemClock") Clock clock) {

        this.messageValidator = messageValidator;
        this.payloadReader = payloadReader;
        this.payloadValidator = payloadValidator;
        this.processedEventRegistrationService = processedEventRegistrationService;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingConfirmedOutboxFactory = bookingConfirmedOutboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result handle(String partitionKey, OutboxEventMessage message) {

        messageValidator.validateSucceeded(partitionKey, message);

        PaymentSucceededPayload payload = payloadReader.read(message);

        payloadValidator.validateSucceeded(partitionKey, message, payload);

        boolean registered =
                processedEventRegistrationService.register(
                        message.eventId(),
                        BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER,
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

        validatePaymentMatchesBooking(booking, payload);

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(payload.bookingId());

        OffsetDateTime confirmedAt = OffsetDateTime.now(clock);

        booking.confirm(confirmedAt);

        Booking savedBooking = bookingRepository.save(booking);

        OutboxEventEntity bookingConfirmedEvent =
                bookingConfirmedOutboxFactory.create(
                        savedBooking, bookingSeats, payload.paymentId(), message);

        outboxService.save(bookingConfirmedEvent);

        return Result.confirmed();
    }

    private static void validatePaymentMatchesBooking(
            Booking booking, PaymentSucceededPayload payload) {

        BigDecimal bookingAmount = booking.getTotalAmount();

        if (bookingAmount == null
                || bookingAmount.compareTo(payload.amount()) != 0
                || booking.getCurrency() == null
                || !booking.getCurrency().equals(payload.currency())) {

            throw new ConflictException(BookingErrorCode.PAYMENT_RESULT_MISMATCH);
        }
    }
}
