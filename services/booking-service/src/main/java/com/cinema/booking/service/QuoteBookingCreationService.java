package com.cinema.booking.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.SeatReservationRequestedOutboxFactory;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.service.OutboxService;

@Service
public class QuoteBookingCreationService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository seatRepository;
    private final BookingMapper mapper;
    private final BookingProperties properties;
    private final SeatReservationRequestedOutboxFactory outboxFactory;
    private final OutboxService outboxService;
    private final Clock clock;

    public QuoteBookingCreationService(
            BookingRepository bookingRepository,
            BookingSeatRepository seatRepository,
            BookingMapper mapper,
            BookingProperties properties,
            SeatReservationRequestedOutboxFactory outboxFactory,
            OutboxService outboxService,
            Clock clock) {

        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.mapper = mapper;
        this.properties = properties;
        this.outboxFactory = outboxFactory;
        this.outboxService = outboxService;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse create(
            UUID userId, String clientRequestId, String fingerprint, BookingQuoteResponse quote) {

        validateQuote(quote);

        OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        OffsetDateTime expiresAt =
                earlier(
                                earlier(
                                        now.plus(properties.reservationExpiration()),
                                        quote.expiresAt()),
                                quote.localStartsAt())
                        .truncatedTo(ChronoUnit.MICROS);

        if (!quote.saleOpen() || !expiresAt.isAfter(now)) {
            throw new ConflictException(BookingErrorCode.QUOTE_NOT_BOOKABLE);
        }

        List<String> numbers =
                quote.items().stream().map(line -> normalize(line.seatNumber())).sorted().toList();

        Booking booking =
                new Booking(
                        userId, quote.showtimeId(), clientRequestId, fingerprint, expiresAt, now);

        booking.assignPricingQuote(quote.quoteId());

        Booking saved = bookingRepository.saveAndFlush(booking);

        List<BookingSeat> seats =
                numbers.stream()
                        .map(
                                number ->
                                        new BookingSeat(
                                                saved.getId(), saved.getShowtimeId(), number))
                        .toList();

        List<BookingSeat> savedSeats = seatRepository.saveAll(seats);

        outboxService.save(outboxFactory.create(saved, savedSeats, now));

        return mapper.toResponse(saved, savedSeats);
    }

    private void validateQuote(BookingQuoteResponse quote) {
        if (quote == null
                || quote.quoteId() == null
                || quote.showtimeId() == null
                || quote.expiresAt() == null
                || quote.localStartsAt() == null
                || quote.items() == null
                || quote.items().isEmpty()
                || quote.seatsHeld()
                || !"VND".equals(quote.currency())) {
            throw unavailable();
        }

        if (quote.items().size() > properties.maxSeatsPerBooking()) {
            throw new ValidationException(BookingErrorCode.TOO_MANY_SEATS);
        }

        Set<String> numbers = new HashSet<>();
        Set<UUID> ids = new HashSet<>();

        for (var line : quote.items()) {
            if (line == null
                    || line.showSeatId() == null
                    || line.seatNumber() == null
                    || line.seatNumber().isBlank()
                    || line.seatNumber().length() > 20
                    || !ids.add(line.showSeatId())
                    || !numbers.add(normalize(line.seatNumber()))) {
                throw unavailable();
            }
        }
    }

    private static String normalize(String number) {
        return number.trim().toUpperCase(Locale.ROOT);
    }

    private static OffsetDateTime earlier(OffsetDateTime first, OffsetDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private static InternalServerException unavailable() {
        return new InternalServerException(BookingErrorCode.QUOTE_SERVICE_UNAVAILABLE);
    }
}
