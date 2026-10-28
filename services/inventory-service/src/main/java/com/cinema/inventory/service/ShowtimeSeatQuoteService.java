package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.response.ShowtimeSeatQuoteResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatQuoteResponse.SeatQuote;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ShowtimeSeatQuoteService {

    private static final int MAX_QUOTE_SEATS = 100;

    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    public ShowtimeSeatQuoteService(
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            Clock clock) {

        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ShowtimeSeatQuoteResponse quote(UUID showtimeId, List<String> seatNumbers) {

        List<String> normalized = normalize(showtimeId, seatNumbers);

        if (!showtimeRepository.existsById(showtimeId)) {
            throw new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND);
        }

        List<ShowSeat> selected = showSeatRepository.findAllForQuote(showtimeId, normalized);

        Map<String, ShowSeat> byNumber = new HashMap<>();

        for (ShowSeat seat : selected) {
            if (seat.getSeatNumber() == null) {
                throw inconsistent();
            }

            String number = normalizeNumber(seat.getSeatNumber());

            if (byNumber.putIfAbsent(number, seat) != null) {
                throw inconsistent();
            }
        }

        if (!byNumber.keySet().equals(new HashSet<>(normalized))) {
            throw new NotFoundException(InventoryErrorCode.SHOW_SEAT_NOT_FOUND);
        }

        List<SeatQuote> lines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int capacity = 0;

        for (String number : normalized) {
            ShowSeat seat = byNumber.get(number);

            if (seat.getId() == null
                    || seat.getSeatType() == null
                    || seat.getStatus() == null
                    || seat.getPrice() == null
                    || seat.getPrice().signum() < 0) {

                throw inconsistent();
            }

            if (seat.getStatus() != ShowSeatStatus.AVAILABLE) {
                throw new ConflictException(InventoryErrorCode.SHOW_SEAT_NOT_AVAILABLE);
            }

            int seatCapacity = seat.getSeatType().getCapacity();

            lines.add(
                    new SeatQuote(
                            seat.getId(),
                            seat.getSeatNumber(),
                            seat.getSeatType(),
                            seatCapacity,
                            seat.getPrice()));

            // COUPLE already stores the entire pair price.
            totalAmount = totalAmount.add(seat.getPrice());
            capacity += seatCapacity;
        }

        OffsetDateTime serverTime = OffsetDateTime.now(clock);

        boolean bookable =
                showtimeRepository.existsHoldEligibleShowtime(
                        showtimeId, serverTime, ShowtimeStatus.OPEN_FOR_BOOKING);

        if (!bookable) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        return new ShowtimeSeatQuoteResponse(
                showtimeId,
                serverTime,
                InventoryEventContract.CURRENCY_VND,
                lines.size(),
                capacity,
                totalAmount,
                List.copyOf(lines));
    }

    private static List<String> normalize(UUID showtimeId, List<String> seatNumbers) {

        if (showtimeId == null
                || seatNumbers == null
                || seatNumbers.isEmpty()
                || seatNumbers.size() > MAX_QUOTE_SEATS) {

            throw invalid();
        }

        List<String> normalized = new ArrayList<>();

        for (String number : seatNumbers) {
            if (number == null || number.isBlank()) {
                throw invalid();
            }

            String value = normalizeNumber(number);

            if (value.isBlank() || value.length() > 20) {
                throw invalid();
            }

            normalized.add(value);
        }

        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw invalid();
        }

        return List.copyOf(normalized);
    }

    private static String normalizeNumber(String number) {
        return number.trim().toUpperCase(Locale.ROOT);
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_SEAT_QUOTE);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }
}
