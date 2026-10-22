package com.cinema.booking.service;

import com.cinema.booking.client.InventoryQuoteClient;
import com.cinema.booking.dto.request.CreateBookingFromQuoteRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.idempotency.BookingRequestFingerprint;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class BookingFromQuoteService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final BookingMapper mapper;
    private final BookingRequestFingerprint fingerprints;
    private final InventoryQuoteClient quoteClient;
    private final QuoteBookingCreationService creationService;
    private final boolean enabled;

    public BookingFromQuoteService(
            BookingRepository bookingRepository,
            BookingSeatRepository seatRepository,
            BookingMapper mapper,
            BookingRequestFingerprint fingerprints,
            InventoryQuoteClient quoteClient,
            QuoteBookingCreationService creationService,
            @Value("${cinema.booking.quote-booking.enabled:false}") boolean enabled) {

        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.mapper = mapper;
        this.fingerprints = fingerprints;
        this.quoteClient = quoteClient;
        this.creationService = creationService;
        this.enabled = enabled;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public BookingResponse create(
            UUID userId, CreateBookingFromQuoteRequest request, String authorization) {

        if (userId == null) {
            throw new ValidationException(BookingErrorCode.USER_ID_REQUIRED);
        }

        if (request == null
                || request.clientRequestId() == null
                || request.clientRequestId().isBlank()
                || request.clientRequestId().length() > 100) {
            throw new ValidationException(BookingErrorCode.CLIENT_REQUEST_ID_REQUIRED);
        }

        if (request.quoteId() == null || request.quoteId().version() != 7) {
            throw new ValidationException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
        }

        String clientRequestId = request.clientRequestId().trim();

        String fingerprint = fingerprints.generateFromQuote(request.quoteId());

        var existing = bookingRepository.findByUserIdAndClientRequestId(userId, clientRequestId);

        if (existing.isPresent()) {
            return resolveExisting(existing.get(), request.quoteId(), fingerprint);
        }

        if (!enabled) {
            throw new ConflictException(BookingErrorCode.QUOTE_BOOKING_NOT_READY);
        }

        // Forward the user JWT. Inventory verifies quote ownership.
        // No database transaction is held during this HTTP call.
        var quote = quoteClient.get(request.quoteId(), authorization);

        if (bookingRepository.existsByPricingQuoteId(request.quoteId())) {
            throw new ConflictException(BookingErrorCode.QUOTE_ALREADY_USED);
        }

        try {
            return creationService.create(userId, clientRequestId, fingerprint, quote);
        } catch (DataIntegrityViolationException exception) {
            // The separate creation transaction has already rolled back.
            var concurrent =
                    bookingRepository.findByUserIdAndClientRequestId(userId, clientRequestId);

            if (concurrent.isPresent()) {
                return resolveExisting(concurrent.get(), request.quoteId(), fingerprint);
            }

            if (bookingRepository.existsByPricingQuoteId(request.quoteId())) {
                throw new ConflictException(BookingErrorCode.QUOTE_ALREADY_USED);
            }

            throw exception;
        }
    }

    private BookingResponse resolveExisting(Booking booking, UUID quoteId, String fingerprint) {

        if (!quoteId.equals(booking.getPricingQuoteId())
                || !fingerprint.equals(booking.getRequestFingerprint())) {
            throw new ConflictException(BookingErrorCode.CLIENT_REQUEST_ID_PAYLOAD_MISMATCH);
        }

        return mapper.toResponse(
                booking, seatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId()));
    }
}
