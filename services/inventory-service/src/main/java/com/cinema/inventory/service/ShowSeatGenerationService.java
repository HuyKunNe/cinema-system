package com.cinema.inventory.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;

@Service
public class ShowSeatGenerationService {

    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatPricingPolicy seatPricingPolicy;

    public ShowSeatGenerationService(
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository,
            SeatPricingPolicy seatPricingPolicy) {

        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatPricingPolicy = seatPricingPolicy;
    }

    @Transactional
    public List<ShowSeat> generate(
            Showtime showtime,
            BigDecimal basePrice) {

        validateShowtime(showtime);

        List<Seat> activeSeats = seatRepository
                .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                        showtime.getRoom().getId());

        if (activeSeats.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.NO_ACTIVE_SEATS);
        }

        Set<UUID> existingSeatIds = findExistingSeatIds(showtime.getId());

        List<ShowSeat> generatedShowSeats = activeSeats.stream()
                .filter(seat -> !existingSeatIds.contains(
                        seat.getId()))
                .map(seat -> createShowSeat(
                        showtime,
                        seat,
                        basePrice))
                .toList();

        if (generatedShowSeats.isEmpty()) {
            return List.of();
        }

        return showSeatRepository.saveAll(
                generatedShowSeats);
    }

    private Set<UUID> findExistingSeatIds(
            UUID showtimeId) {

        List<ShowSeat> existingShowSeats = showSeatRepository
                .findAllByShowtime_IdOrderBySeatNumberAsc(
                        showtimeId);

        Set<UUID> existingSeatIds = new HashSet<>();

        for (ShowSeat showSeat : existingShowSeats) {
            existingSeatIds.add(
                    showSeat.getSeat().getId());
        }

        return existingSeatIds;
    }

    private ShowSeat createShowSeat(
            Showtime showtime,
            Seat seat,
            BigDecimal basePrice) {

        BigDecimal price = seatPricingPolicy.calculate(
                basePrice,
                seat.getSeatType());

        ShowSeat showSeat = new ShowSeat(
                showtime,
                seat,
                price);

        showtime.addShowSeat(showSeat);

        return showSeat;
    }

    private void validateShowtime(Showtime showtime) {
        if (showtime == null) {
            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_REQUIRED);
        }

        if (showtime.getId() == null) {
            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_NOT_PERSISTED);
        }

        if (showtime.getRoom() == null
                || showtime.getRoom().getId() == null) {

            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }
    }
}
