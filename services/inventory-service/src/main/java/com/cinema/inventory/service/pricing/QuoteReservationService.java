package com.cinema.inventory.service.pricing;

import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.PricingQuote;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.PricingQuoteRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class QuoteReservationService {

    private static final long MAX_AMOUNT_VND = 99_999_999_999_999_999L;

    private final PricingQuoteRepository quoteRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingQuoteQueryService queryService;
    private final Clock clock;

    public QuoteReservationService(
            PricingQuoteRepository quoteRepository,
            CinemaRepository cinemaRepository,
            RoomRepository roomRepository,
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            BookingQuoteQueryService queryService,
            Clock clock) {

        this.quoteRepository = quoteRepository;
        this.cinemaRepository = cinemaRepository;
        this.roomRepository = roomRepository;
        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.queryService = queryService;
        this.clock = clock;
    }

    /**
     * Must run inside the reservation consumer transaction. Caller must persist its processed-event
     * record and outcome outbox in that same transaction.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Reservation reserve(
            UUID userId,
            UUID bookingId,
            UUID quoteId,
            UUID expectedShowtimeId,
            OffsetDateTime requestedHoldExpiresAt) {

        if (userId == null
                || bookingId == null
                || quoteId == null
                || expectedShowtimeId == null
                || requestedHoldExpiresAt == null) {
            throw new ValidationException(InventoryErrorCode.QUOTE_REQUEST_INVALID);
        }

        UUID showtimeId =
                quoteRepository
                        .findOwnedShowtimeId(quoteId, userId)
                        .orElseThrow(QuoteReservationService::notFound);

        if (!showtimeId.equals(expectedShowtimeId)) {
            throw new ValidationException(InventoryErrorCode.QUOTE_REQUEST_INVALID);
        }

        // Same root order as showtime mutations:
        // cinema -> room -> showtime -> quote -> seats.
        Showtime showtime = lockShowtime(showtimeId);

        PricingQuote quote =
                quoteRepository
                        .findOwnedForUpdate(quoteId, userId)
                        .orElseThrow(QuoteReservationService::notFound);

        BookingQuoteResponse snapshot = queryService.readStoredSnapshot(quote);

        // Replay returns the recorded outcome and never recreates a hold.
        if (quote.isConsumed()) {
            if (!bookingId.equals(quote.getConsumedByBookingId())) {
                throw new ConflictException(InventoryErrorCode.QUOTE_ALREADY_CONSUMED);
            }

            return new Reservation(
                    snapshot, quote.getConsumedAt(), quote.getReservationExpiresAt(), true);
        }

        OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        if (!quote.getExpiresAt().isAfter(now)) {
            throw new ConflictException(InventoryErrorCode.QUOTE_EXPIRED);
        }

        validateShowtime(showtime, quote);

        Set<UUID> ids = new HashSet<>();

        if (snapshot.items().size() > 100) {
            throw invalidSnapshot();
        }

        for (var line : snapshot.items()) {
            if (line.showSeatId() == null || !ids.add(line.showSeatId())) {
                throw invalidSnapshot();
            }
        }

        List<ShowSeat> seats =
                showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(showtimeId, ids);

        if (seats.size() != ids.size()) {
            throw seatUnavailable();
        }

        Map<UUID, ShowSeat> byId =
                seats.stream().collect(Collectors.toMap(ShowSeat::getId, Function.identity()));

        validateLines(snapshot, byId);

        // Recheck time after waiting for seat locks.
        now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

        if (!quote.getExpiresAt().isAfter(now)) {
            throw new ConflictException(InventoryErrorCode.QUOTE_EXPIRED);
        }

        if (!showtime.getStartsAt().isAfter(now)) {
            throw new ConflictException(InventoryErrorCode.QUOTE_SHOWTIME_UNAVAILABLE);
        }

        OffsetDateTime holdExpiresAt =
                earlier(
                                earlier(requestedHoldExpiresAt, quote.getExpiresAt()),
                                showtime.getStartsAt())
                        .truncatedTo(ChronoUnit.MICROS);

        if (!holdExpiresAt.isAfter(now)) {
            throw new ValidationException(InventoryErrorCode.INVALID_HOLD_EXPIRATION);
        }

        // All checks finish before mutating any seat.
        for (ShowSeat seat : seats) {
            seat.holdFromQuote(bookingId, holdExpiresAt, now);
        }

        quote.consume(bookingId, now, holdExpiresAt);

        // Entities are managed. The caller's transaction commits holds,
        // quote consumption and outcome outbox together.
        return new Reservation(snapshot, now, holdExpiresAt, false);
    }

    private Showtime lockShowtime(UUID showtimeId) {
        UUID roomId =
                showtimeRepository
                        .findRoomIdByShowtimeId(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        UUID cinemaId =
                roomRepository
                        .findCinemaIdByRoomId(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        cinemaRepository
                .findByIdForRead(cinemaId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        roomRepository
                .findByIdForRead(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        return showtimeRepository
                .findByIdForQuote(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }

    private void validateShowtime(Showtime showtime, PricingQuote quote) {
        if (!showtime.usesPolicyPricing()
                || !Objects.equals(quote.getShowtimeVersion(), showtime.getVersion())
                || !quote.getPolicyId().equals(showtime.getPricingPolicy().getId())
                || !Objects.equals(
                        Long.valueOf(quote.getPolicyVersion()),
                        showtime.getPricingPolicyVersion())) {
            throw new ConflictException(InventoryErrorCode.QUOTE_STALE);
        }

        showtime.getPricingPolicy().requirePublishedConfiguration();

        if (showtime.getStatus() != ShowtimeStatus.OPEN_FOR_BOOKING
                || !showtime.getRoom().isActive()
                || !showtime.getRoom().getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.QUOTE_SHOWTIME_UNAVAILABLE);
        }
    }

    private void validateLines(BookingQuoteResponse snapshot, Map<UUID, ShowSeat> seats) {

        long total = 0;

        for (var line : snapshot.items()) {
            ShowSeat seat = seats.get(line.showSeatId());

            if (seat == null || seat.getStatus() != ShowSeatStatus.AVAILABLE) {
                throw seatUnavailable();
            }

            if (seat.getPrice() != null
                    || !seat.getSeat().getId().equals(line.seatId())
                    || !seat.getSeatNumber().equals(line.seatNumber())
                    || !seat.getSeatType().name().equals(line.seatType())
                    || seat.getSeatType().getCapacity() != line.capacity()
                    || line.fareRuleId() == null
                    || line.effectiveTicketCategoryCode() == null
                    || line.effectiveTicketCategoryCode().isBlank()) {
                throw invalidSnapshot();
            }

            String expectedUnit = seat.getSeatType() == SeatType.COUPLE ? "PER_PAIR" : "PER_TICKET";

            if (!expectedUnit.equals(line.fareUnit())
                    || line.amountVnd() == null
                    || !line.amountVnd().matches("[0-9]{1,17}")) {
                throw invalidSnapshot();
            }

            try {
                total = Math.addExact(total, Long.parseLong(line.amountVnd()));
            } catch (ArithmeticException exception) {
                throw invalidSnapshot();
            }

            if (total > MAX_AMOUNT_VND) {
                throw invalidSnapshot();
            }
        }

        if (total != Long.parseLong(snapshot.totalAmountVnd())) {
            throw invalidSnapshot();
        }
    }

    private static OffsetDateTime earlier(OffsetDateTime first, OffsetDateTime second) {
        return first.isBefore(second) ? first : second;
    }

    private static NotFoundException notFound() {
        return new NotFoundException(InventoryErrorCode.QUOTE_NOT_FOUND);
    }

    private static ConflictException seatUnavailable() {
        return new ConflictException(InventoryErrorCode.QUOTE_SEAT_UNAVAILABLE);
    }

    private static InternalServerException invalidSnapshot() {
        return new InternalServerException(InventoryErrorCode.QUOTE_SNAPSHOT_INVALID);
    }

    public record Reservation(
            BookingQuoteResponse quote,
            OffsetDateTime heldAt,
            OffsetDateTime holdExpiresAt,
            boolean replayed) {}
}
