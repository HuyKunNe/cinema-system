package com.cinema.booking.service.impl;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingService;
import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.model.PageResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingMapper bookingMapper;

    private final BookingProperties bookingProperties;

    private final Clock clock;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingMapper bookingMapper,
            BookingProperties bookingProperties,
            Clock clock) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingMapper = bookingMapper;
        this.bookingProperties = bookingProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponse create(UUID userId, CreateBookingRequest request) {

        requireUserId(userId);

        List<String> normalizedSeatNumbers = normalizeSeatNumbers(request.seatNumbers());

        validateSeatCount(normalizedSeatNumbers);
        validateNoDuplicateSeats(normalizedSeatNumbers);

        OffsetDateTime now = OffsetDateTime.now(clock);

        OffsetDateTime expiresAt = now.plus(bookingProperties.reservationExpiration());

        Booking booking =
                new Booking(
                        userId, request.showtimeId(), request.clientRequestId(), expiresAt, now);

        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats =
                normalizedSeatNumbers.stream()
                        .map(
                                seatNumber ->
                                        new BookingSeat(
                                                savedBooking.getId(),
                                                savedBooking.getShowtimeId(),
                                                seatNumber))
                        .toList();

        List<BookingSeat> savedSeats = bookingSeatRepository.saveAll(bookingSeats);

        return bookingMapper.toResponse(savedBooking, savedSeats);
    }

    @Override
    public BookingResponse findById(UUID userId, UUID bookingId) {

        requireUserId(userId);
        requireBookingId(bookingId);

        Booking booking =
                bookingRepository
                        .findByIdAndUserId(bookingId, userId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId());

        return bookingMapper.toResponse(booking, seats);
    }

    @Override
    public PageResponse<BookingResponse> findAll(UUID userId, int page, int size) {

        requireUserId(userId);

        PageRequest pageRequest =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Booking> bookings = bookingRepository.findAllByUserId(userId, pageRequest);

        Map<UUID, List<BookingSeat>> seatsByBookingId = loadSeatsByBookingId(bookings.getContent());

        List<BookingResponse> responses =
                bookings.getContent().stream()
                        .map(
                                booking ->
                                        bookingMapper.toResponse(
                                                booking,
                                                seatsByBookingId.getOrDefault(
                                                        booking.getId(), List.of())))
                        .toList();

        Page<BookingResponse> responsePage =
                new PageImpl<>(responses, pageRequest, bookings.getTotalElements());

        return PageResponseMapper.map(responsePage);
    }

    private Map<UUID, List<BookingSeat>> loadSeatsByBookingId(List<Booking> bookings) {

        if (bookings.isEmpty()) {
            return Map.of();
        }

        List<UUID> bookingIds = bookings.stream().map(Booking::getId).toList();

        return bookingSeatRepository
                .findAllByBookingIdInOrderByBookingIdAscSeatNumberAsc(bookingIds)
                .stream()
                .collect(Collectors.groupingBy(BookingSeat::getBookingId));
    }

    private List<String> normalizeSeatNumbers(Collection<String> seatNumbers) {

        if (seatNumbers == null || seatNumbers.isEmpty()) {
            throw new ValidationException(BookingErrorCode.SEAT_NUMBERS_REQUIRED);
        }

        return seatNumbers.stream().map(this::normalizeSeatNumber).toList();
    }

    private String normalizeSeatNumber(String seatNumber) {

        if (seatNumber == null || seatNumber.isBlank()) {
            throw new ValidationException(BookingErrorCode.SEAT_NUMBER_REQUIRED);
        }

        return seatNumber.trim().toUpperCase(Locale.ROOT);
    }

    private void validateSeatCount(List<String> seatNumbers) {

        if (seatNumbers.size() > bookingProperties.maxSeatsPerBooking()) {

            throw new ValidationException(BookingErrorCode.TOO_MANY_SEATS);
        }
    }

    private void validateNoDuplicateSeats(List<String> seatNumbers) {

        if (new LinkedHashSet<>(seatNumbers).size() != seatNumbers.size()) {

            throw new ValidationException(BookingErrorCode.DUPLICATE_SEAT_NUMBER);
        }
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
