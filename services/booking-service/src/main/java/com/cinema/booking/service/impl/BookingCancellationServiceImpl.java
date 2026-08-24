package com.cinema.booking.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.booking.event.BookingCancelledOutboxFactory;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.service.OutboxService;

@Service
public class BookingCancellationServiceImpl implements BookingCancellationService {

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingMapper bookingMapper;

    private final BookingCancelledOutboxFactory outboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public BookingCancellationServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingMapper bookingMapper,
            BookingCancelledOutboxFactory outboxFactory,
            OutboxService outboxService,
            @Qualifier("systemClock") Clock clock) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingMapper = bookingMapper;
        this.outboxFactory = outboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID userId, UUID bookingId) {

        requireUserId(userId);
        requireBookingId(bookingId);

        Booking booking =
                bookingRepository
                        .findByIdAndUserIdForUpdate(bookingId, userId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now(clock);

        booking.cancel(now);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        OutboxEventEntity outboxEvent =
                outboxFactory.create(savedBooking, BookingCancellationReason.USER_REQUESTED);

        outboxService.save(outboxEvent);

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(savedBooking.getId());

        return bookingMapper.toResponse(savedBooking, bookingSeats);
    }

    private static void requireUserId(UUID userId) {

        if (userId == null) {
            throw new ValidationException(BookingErrorCode.USER_ID_REQUIRED);
        }
    }

    private static void requireBookingId(UUID bookingId) {

        if (bookingId == null) {
            throw new ValidationException(BookingErrorCode.BOOKING_ID_REQUIRED);
        }
    }
}
