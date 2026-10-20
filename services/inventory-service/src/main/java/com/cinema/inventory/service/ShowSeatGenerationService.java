package com.cinema.inventory.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
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
    private final ShowtimeLayoutService showtimeLayoutService;

    public ShowSeatGenerationService(
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository,
            SeatPricingPolicy seatPricingPolicy,
            ShowtimeLayoutService showtimeLayoutService) {

        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatPricingPolicy = seatPricingPolicy;
        this.showtimeLayoutService = showtimeLayoutService;
    }

    @Transactional
    public List<ShowSeat> generate(Showtime showtime, BigDecimal basePrice) {

        validateShowtime(showtime);

        List<LayoutSeatSnapshot> snapshots;

        if (showtime.getRoomLayout() == null) {
            snapshots =
                    seatRepository
                            .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                                    showtime.getRoom().getId())
                            .stream()
                            .map(LayoutSeatSnapshot::fromSeat)
                            .toList();

            if (snapshots.isEmpty()) {
                throw new ConflictException(InventoryErrorCode.NO_ACTIVE_SEATS);
            }
        } else {
            snapshots = showtimeLayoutService.getSeatSnapshotsForGeneration(showtime);
        }

        List<ShowSeat> existingShowSeats =
                showSeatRepository.findAllByShowtime_IdOrderBySeatNumberAsc(showtime.getId());

        if (showtime.getRoomLayout() != null) {
            validateExistingSnapshots(snapshots, existingShowSeats);
        }

        Set<UUID> existingSeatIds = new HashSet<>();

        for (ShowSeat showSeat : existingShowSeats) {
            existingSeatIds.add(showSeat.getSeat().getId());
        }

        List<ShowSeat> generatedShowSeats =
                snapshots.stream()
                        .filter(snapshot -> !existingSeatIds.contains(snapshot.seat().getId()))
                        .map(snapshot -> createShowSeat(showtime, snapshot, basePrice))
                        .toList();

        if (generatedShowSeats.isEmpty()) {
            return List.of();
        }

        return showSeatRepository.saveAll(generatedShowSeats);
    }

    private void validateExistingSnapshots(
            List<LayoutSeatSnapshot> snapshots, List<ShowSeat> existingShowSeats) {

        Map<UUID, LayoutSeatSnapshot> snapshotsBySeatId = new HashMap<>();

        for (LayoutSeatSnapshot snapshot : snapshots) {
            snapshotsBySeatId.put(snapshot.seat().getId(), snapshot);
        }

        for (ShowSeat showSeat : existingShowSeats) {
            LayoutSeatSnapshot snapshot = snapshotsBySeatId.get(showSeat.getSeat().getId());

            if (snapshot == null) {
                throw new ConflictException(InventoryErrorCode.SHOWTIME_LAYOUT_SEATS_MISMATCH);
            }

            if (!Objects.equals(showSeat.getSeatNumber(), snapshot.seatNumber())
                    || showSeat.getSeatType() != snapshot.seatType()) {
                throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
            }
        }
    }

    private ShowSeat createShowSeat(
            Showtime showtime, LayoutSeatSnapshot snapshot, BigDecimal basePrice) {

        BigDecimal price = seatPricingPolicy.calculate(basePrice, snapshot.seatType());

        ShowSeat showSeat =
                new ShowSeat(
                        showtime,
                        snapshot.seat(),
                        snapshot.seatNumber(),
                        snapshot.seatType(),
                        price);

        showtime.addShowSeat(showSeat);

        return showSeat;
    }

    private void validateShowtime(Showtime showtime) {
        if (showtime == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_REQUIRED);
        }

        if (showtime.getId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_NOT_PERSISTED);
        }

        if (showtime.getRoom() == null || showtime.getRoom().getId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }
    }
}
