package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowSeatGenerationService {

    private final SeatRepository seatRepository;
    private final ShowSeatRepository showSeatRepository;
    private final SeatPricingPolicy seatPricingPolicy;
    private final ShowtimeLayoutService showtimeLayoutService;
    private final FareMatrixService fareMatrixService;

    public ShowSeatGenerationService(
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository,
            SeatPricingPolicy seatPricingPolicy,
            ShowtimeLayoutService showtimeLayoutService,
            FareMatrixService fareMatrixService) {

        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.seatPricingPolicy = seatPricingPolicy;
        this.showtimeLayoutService = showtimeLayoutService;
        this.fareMatrixService = fareMatrixService;
    }

    @Transactional
    public List<ShowSeat> generate(Showtime showtime, BigDecimal basePrice) {

        validateShowtime(showtime);

        List<LayoutSeatSnapshot> snapshots = loadSnapshots(showtime);

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
                        .map(
                                snapshot ->
                                        createShowSeatWithPrice(
                                                showtime,
                                                snapshot,
                                                seatPricingPolicy.calculate(
                                                        basePrice, snapshot.seatType())))
                        .toList();

        if (generatedShowSeats.isEmpty()) {
            return List.of();
        }

        return showSeatRepository.saveAll(generatedShowSeats);
    }

    @Transactional
    public List<ShowSeat> generateFromPublishedPricing(Showtime showtime) {
        validateScheduledShowtime(showtime);

        var selected = showtime.getPricingPolicyVersion();

        if (selected == null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_REQUIRED);
        }

        showtime.replacePricingPolicyVersion(selected);

        if (showSeatRepository.existsByShowtime_Id(showtime.getId())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_GENERATION_LOCKED);
        }

        if (showtime.getRoom().getRoomType() == null
                || showtime.getScreeningVersion() == null
                || showtime.getScreeningVersion().getContentFormat() == null) {

            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_CONTEXT_REQUIRED);
        }

        List<LayoutSeatSnapshot> snapshots = loadSnapshots(showtime);

        var matrix =
                fareMatrixService.getForShowtimePricing(
                        showtime.getRoom().getCinema().getId(),
                        selected.getPolicy().getId(),
                        selected.getId());

        var experience = showtime.getRoom().getRoomType().getPricingExperienceType();

        var format = showtime.getScreeningVersion().getContentFormat();

        var localStartsAt =
                showtime.getStartsAt()
                        .atZoneSameInstant(
                                PricingFareResolver.cinemaZone(
                                        showtime.getRoom().getCinema().getTimeZone()))
                        .toLocalTime();

        Map<SeatType, BigDecimal> prices = new EnumMap<>(SeatType.class);

        for (LayoutSeatSnapshot snapshot : snapshots) {
            SeatType seatType = snapshot.seatType();

            if (prices.containsKey(seatType)) {
                continue;
            }

            var fare =
                    PricingFareResolver.resolve(
                            matrix, localStartsAt, experience, format, null, seatType);

            prices.put(seatType, fare.amount());
        }

        List<ShowSeat> generatedShowSeats =
                snapshots.stream()
                        .map(
                                snapshot ->
                                        createShowSeatWithPrice(
                                                showtime,
                                                snapshot,
                                                prices.get(snapshot.seatType())))
                        .toList();

        return showSeatRepository.saveAll(generatedShowSeats);
    }

    private List<LayoutSeatSnapshot> loadSnapshots(Showtime showtime) {
        List<LayoutSeatSnapshot> snapshots;

        if (showtime.getRoomLayout() == null) {
            snapshots =
                    seatRepository
                            .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
                                    showtime.getRoom().getId())
                            .stream()
                            .map(LayoutSeatSnapshot::fromSeat)
                            .toList();
        } else {
            snapshots = showtimeLayoutService.getSeatSnapshotsForGeneration(showtime);
        }

        if (snapshots.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.NO_ACTIVE_SEATS);
        }

        return snapshots;
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

    private ShowSeat createShowSeatWithPrice(
            Showtime showtime, LayoutSeatSnapshot snapshot, BigDecimal price) {

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
        validateScheduledShowtime(showtime);

        if (showtime.getPricingPolicyVersion() != null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_GENERATION_LOCKED);
        }
    }

    private void validateScheduledShowtime(Showtime showtime) {
        if (showtime == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_REQUIRED);
        }

        if (showtime.getId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_NOT_PERSISTED);
        }

        if (showtime.getRoom() == null || showtime.getRoom().getId() == null) {

            throw new ValidationException(InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }
    }
}
