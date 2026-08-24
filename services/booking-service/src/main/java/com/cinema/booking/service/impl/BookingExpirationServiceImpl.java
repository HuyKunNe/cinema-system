package com.cinema.booking.service.impl;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingExpiredOutboxFactory;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.BookingExpirationService;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class BookingExpirationServiceImpl implements BookingExpirationService {

    private final BookingRepository bookingRepository;

    private final BookingExpiredOutboxFactory outboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public BookingExpirationServiceImpl(
            BookingRepository bookingRepository,
            BookingExpiredOutboxFactory outboxFactory,
            OutboxService outboxService,
            @Qualifier("systemClock") Clock clock) {

        this.bookingRepository = bookingRepository;
        this.outboxFactory = outboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean expireIfDue(UUID bookingId) {

        requireBookingId(bookingId);

        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);

        if (booking == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        if (!isExpirationDue(booking, now)) {
            return false;
        }

        booking.expire(now);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        OutboxEventEntity outboxEvent = outboxFactory.create(savedBooking, now);

        outboxService.save(outboxEvent);

        return true;
    }

    private static boolean isExpirationDue(Booking booking, OffsetDateTime now) {

        return booking.getStatus() == BookingStatus.RESERVED
                && !booking.getExpiresAt().isAfter(now);
    }

    private static void requireBookingId(UUID bookingId) {

        if (bookingId == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }
    }
}
