package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.QuoteShowtimeSeatsRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.dto.response.ShowtimeGroupedSeatQuoteResponse;
import com.cinema.inventory.dto.response.ShowtimeGroupedSeatQuoteResponse.SeatQuote;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ShowtimeGroupedSeatQuoteService {

    private final ShowtimeSeatQuoteService seatQuoteService;
    private final ShowtimeRepository showtimeRepository;
    private final FareMatrixService fareMatrixService;
    private final Clock clock;

    public ShowtimeGroupedSeatQuoteService(
            ShowtimeSeatQuoteService seatQuoteService,
            ShowtimeRepository showtimeRepository,
            FareMatrixService fareMatrixService,
            Clock clock) {

        this.seatQuoteService = seatQuoteService;
        this.showtimeRepository = showtimeRepository;
        this.fareMatrixService = fareMatrixService;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ShowtimeGroupedSeatQuoteResponse quote(
            UUID showtimeId, QuoteShowtimeSeatsRequest request) {

        Map<String, UUID> requestedGroups = normalize(request);

        var availableSeats =
                seatQuoteService.quote(showtimeId, List.copyOf(requestedGroups.keySet()));

        var showtime =
                showtimeRepository
                        .findByIdForSeatMap(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        var selected = showtime.getPricingPolicyVersion();

        if (selected == null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_REQUIRED);
        }

        var room = showtime.getRoom();
        var cinema = room.getCinema();
        var screening = showtime.getScreeningVersion();

        if (room.getRoomType() == null
                || screening == null
                || screening.getContentFormat() == null) {

            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_CONTEXT_REQUIRED);
        }

        var matrix =
                fareMatrixService.get(
                        cinema.getId(), selected.getPolicy().getId(), selected.getId());

        if (matrix.status() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED);
        }

        if (!matrix.coverageComplete()) {
            throw inconsistent();
        }

        var localTime =
                showtime.getStartsAt()
                        .atZoneSameInstant(PricingFareResolver.cinemaZone(cinema.getTimeZone()))
                        .toLocalTime();

        var timeBand = PricingFareResolver.resolveTimeBand(matrix, localTime);

        var experience = room.getRoomType().getPricingExperienceType();
        var format = screening.getContentFormat();

        Map<FareKey, CellResponse> resolvedFares = new HashMap<>();
        List<SeatQuote> lines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (var seat : availableSeats.seats()) {
            String number = normalizeNumber(seat.seatNumber());

            UUID requestedGroupId = requestedGroups.get(number);

            UUID groupId =
                    requestedGroupId == null ? matrix.defaultTicketGroupId() : requestedGroupId;

            FareKey key = new FareKey(seat.seatType(), groupId);

            CellResponse fare =
                    resolvedFares.computeIfAbsent(
                            key,
                            ignored ->
                                    PricingFareResolver.resolve(
                                            matrix,
                                            localTime,
                                            experience,
                                            format,
                                            groupId,
                                            seat.seatType()));

            FareUnit expectedUnit =
                    seat.seatType() == SeatType.COUPLE ? FareUnit.PAIR : FareUnit.SEAT;

            if (fare.amount() == null
                    || fare.amount().signum() < 0
                    || fare.unit() != expectedUnit) {

                throw inconsistent();
            }

            var group =
                    matrix.ticketGroups().stream()
                            .filter(candidate -> candidate.id().equals(fare.ticketGroupId()))
                            .findFirst()
                            .orElseThrow(ShowtimeGroupedSeatQuoteService::inconsistent);

            lines.add(
                    new SeatQuote(
                            seat.showSeatId(),
                            seat.seatNumber(),
                            seat.seatType(),
                            seat.capacity(),
                            group.id(),
                            group.name(),
                            group.eligibilityDescription(),
                            fare.unit(),
                            fare.amount()));

            // One COUPLE inventory seat already represents the whole pair.
            totalAmount = totalAmount.add(fare.amount());
        }

        OffsetDateTime serverTime = OffsetDateTime.now(clock);

        boolean bookable =
                showtimeRepository.existsHoldEligibleShowtime(
                        showtimeId, serverTime, ShowtimeStatus.OPEN_FOR_BOOKING);

        if (!bookable) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        return new ShowtimeGroupedSeatQuoteResponse(
                showtimeId,
                matrix.policyVersionId(),
                timeBand.id(),
                availableSeats.currency(),
                serverTime,
                lines.size(),
                availableSeats.capacity(),
                totalAmount,
                List.copyOf(lines));
    }

    private static Map<String, UUID> normalize(QuoteShowtimeSeatsRequest request) {

        if (request == null
                || request.seats() == null
                || request.seats().isEmpty()
                || request.seats().size() > 100) {

            throw invalid();
        }

        Map<String, UUID> result = new LinkedHashMap<>();

        for (var selection : request.seats()) {
            if (selection == null
                    || selection.seatNumber() == null
                    || selection.seatNumber().isBlank()) {

                throw invalid();
            }

            String number = normalizeNumber(selection.seatNumber());

            if (number.isBlank() || number.length() > 20 || result.containsKey(number)) {

                throw invalid();
            }

            result.put(number, selection.ticketGroupId());
        }

        return result;
    }

    private static String normalizeNumber(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_SEAT_QUOTE);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }

    private record FareKey(SeatType seatType, UUID ticketGroupId) {}
}

