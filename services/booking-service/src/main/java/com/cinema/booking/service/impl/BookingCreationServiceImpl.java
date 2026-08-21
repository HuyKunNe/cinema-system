package com.cinema.booking.service.impl;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.SeatReservationRequestedOutboxFactory;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCreationService;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.service.OutboxService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingCreationServiceImpl implements BookingCreationService {

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingMapper bookingMapper;

    private final BookingProperties bookingProperties;

    private final SeatReservationRequestedOutboxFactory outboxFactory;

    private final OutboxService outboxService;

    private final Clock clock;

    public BookingCreationServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingMapper bookingMapper,
            BookingProperties bookingProperties,
            SeatReservationRequestedOutboxFactory outboxFactory,
            OutboxService outboxService,
            Clock clock) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingMapper = bookingMapper;
        this.bookingProperties = bookingProperties;
        this.outboxFactory = outboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponse createNew(
            UUID userId,
            UUID showtimeId,
            String clientRequestId,
            String requestFingerprint,
            List<String> normalizedSeatNumbers) {

        OffsetDateTime now = OffsetDateTime.now(clock);

        OffsetDateTime expiresAt = now.plus(bookingProperties.reservationExpiration());

        Booking booking =
                new Booking(
                        userId, showtimeId, clientRequestId, requestFingerprint, expiresAt, now);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        List<BookingSeat> bookingSeats =
                normalizedSeatNumbers.stream()
                        .map(
                                seatNumber ->
                                        new BookingSeat(
                                                savedBooking.getId(), showtimeId, seatNumber))
                        .toList();

        List<BookingSeat> savedSeats = bookingSeatRepository.saveAll(bookingSeats);

        OutboxEventEntity outboxEvent = outboxFactory.create(savedBooking, savedSeats, now);

        outboxService.save(outboxEvent);

        return bookingMapper.toResponse(savedBooking, savedSeats);
    }
}
