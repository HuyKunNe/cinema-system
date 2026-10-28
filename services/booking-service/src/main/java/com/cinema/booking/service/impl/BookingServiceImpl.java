package com.cinema.booking.service.impl;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.request.BookingSeatSelectionRequest;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.idempotency.BookingRequestFingerprint;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCreationService;
import com.cinema.booking.service.BookingService;
import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.model.PageResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    private final BookingSeatRepository bookingSeatRepository;

    private final BookingCreationService bookingCreationService;

    private final BookingRequestFingerprint requestFingerprint;

    private final BookingMapper bookingMapper;

    private final BookingProperties bookingProperties;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            BookingCreationService bookingCreationService,
            BookingRequestFingerprint requestFingerprint,
            BookingMapper bookingMapper,
            BookingProperties bookingProperties) {

        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.bookingCreationService = bookingCreationService;
        this.requestFingerprint = requestFingerprint;
        this.bookingMapper = bookingMapper;
        this.bookingProperties = bookingProperties;
    }

    @Override
    public BookingResponse create(UUID userId, CreateBookingRequest request) {

        requireUserId(userId);

        if (request == null || !request.isSeatSelectionValid()) {
            throw new ValidationException(BookingErrorCode.INVALID_SEAT_SELECTION);
        }

        if (request.showtimeId() == null) {
            throw new ValidationException(BookingErrorCode.SHOWTIME_ID_REQUIRED);
        }

        String clientRequestId = normalizeClientRequestId(request.clientRequestId());

        List<BookingSeatSelectionRequest> selections = normalizeSeatSelections(request);

        List<String> numbers =
                selections.stream().map(BookingSeatSelectionRequest::seatNumber).toList();

        validateSeatCount(numbers);
        validateNoDuplicateSeats(numbers);

        List<BookingSeatSelectionRequest> sortedSelections =
                selections.stream()
                        .sorted(Comparator.comparing(BookingSeatSelectionRequest::seatNumber))
                        .toList();

        List<String> sortedNumbers =
                sortedSelections.stream().map(BookingSeatSelectionRequest::seatNumber).toList();

        BigDecimal expectedTotalAmount = validateExpectedTotalAmount(request.expectedTotalAmount());

        boolean hasExplicitGroups =
                sortedSelections.stream().anyMatch(seat -> seat.ticketGroupId() != null);

        String fingerprint =
                hasExplicitGroups
                        ? requestFingerprint.generateWithSeatSelections(
                                request.showtimeId(), sortedSelections, expectedTotalAmount)
                        : expectedTotalAmount == null
                                ? requestFingerprint.generate(request.showtimeId(), sortedNumbers)
                                : requestFingerprint.generate(
                                        request.showtimeId(), sortedNumbers, expectedTotalAmount);

        return bookingRepository
                .findByUserIdAndClientRequestId(userId, clientRequestId)
                .map(booking -> resolveExisting(booking, fingerprint))
                .orElseGet(
                        () ->
                                createOrResolveConcurrentRequest(
                                        userId,
                                        request.showtimeId(),
                                        clientRequestId,
                                        fingerprint,
                                        sortedSelections,
                                        expectedTotalAmount));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse findById(UUID userId, UUID bookingId) {

        requireUserId(userId);
        requireBookingId(bookingId);

        Booking booking =
                bookingRepository
                        .findByIdAndUserId(bookingId, userId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));

        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
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

    private BookingResponse createOrResolveConcurrentRequest(
            UUID userId,
            UUID showtimeId,
            String clientRequestId,
            String fingerprint,
            List<BookingSeatSelectionRequest> selections,
            BigDecimal expectedTotalAmount) {

        try {
            boolean hasExplicitGroups =
                    selections.stream().anyMatch(seat -> seat.ticketGroupId() != null);

            if (hasExplicitGroups) {
                return bookingCreationService.createWithSeatSelections(
                        userId,
                        showtimeId,
                        clientRequestId,
                        fingerprint,
                        selections,
                        expectedTotalAmount);
            }

            List<String> numbers =
                    selections.stream().map(BookingSeatSelectionRequest::seatNumber).toList();

            if (expectedTotalAmount == null) {
                return bookingCreationService.createNew(
                        userId, showtimeId, clientRequestId, fingerprint, numbers);
            }

            return bookingCreationService.createNew(
                    userId, showtimeId, clientRequestId, fingerprint, numbers, expectedTotalAmount);

        } catch (DataIntegrityViolationException exception) {
            return bookingRepository
                    .findByUserIdAndClientRequestId(userId, clientRequestId)
                    .map(booking -> resolveExisting(booking, fingerprint))
                    .orElseThrow(() -> exception);
        }
    }

    private static BigDecimal validateExpectedTotalAmount(BigDecimal amount) {

        if (amount == null) {
            return null;
        }

        if (amount.signum() < 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 17) {

            throw new ValidationException(BookingErrorCode.INVALID_EXPECTED_TOTAL_AMOUNT);
        }

        return amount.stripTrailingZeros();
    }

    private BookingResponse resolveExisting(Booking booking, String requestFingerprint) {

        if (!booking.getRequestFingerprint().equals(requestFingerprint)) {

            throw new ConflictException(BookingErrorCode.CLIENT_REQUEST_ID_PAYLOAD_MISMATCH);
        }

        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId());

        return bookingMapper.toResponse(booking, seats);
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

    private String normalizeClientRequestId(String clientRequestId) {

        if (clientRequestId == null || clientRequestId.isBlank()) {

            throw new ValidationException(BookingErrorCode.CLIENT_REQUEST_ID_REQUIRED);
        }

        return clientRequestId.trim();
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

        String normalized = seatNumber.trim().toUpperCase(Locale.ROOT);

        if (normalized.isBlank() || normalized.length() > 20) {
            throw new ValidationException(BookingErrorCode.INVALID_SEAT_SELECTION);
        }

        return normalized;
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

    private List<BookingSeatSelectionRequest> normalizeSeatSelections(
            CreateBookingRequest request) {

        if (request.seatNumbers() != null) {
            return normalizeSeatNumbers(request.seatNumbers()).stream()
                    .map(number -> new BookingSeatSelectionRequest(number, null))
                    .toList();
        }

        return request.seats().stream()
                .map(
                        seat -> {
                            if (seat == null) {
                                throw new ValidationException(
                                        BookingErrorCode.INVALID_SEAT_SELECTION);
                            }

                            return new BookingSeatSelectionRequest(
                                    normalizeSeatNumber(seat.seatNumber()), seat.ticketGroupId());
                        })
                .toList();
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
