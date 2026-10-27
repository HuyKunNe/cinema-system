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
    private final ShowtimeLayoutService showtimeLayoutService;
    private final FareMatrixService fareMatrixService;

    public ShowSeatGenerationService(
            SeatRepository seatRepository,
            ShowSeatRepository showSeatRepository,
            ShowtimeLayoutService showtimeLayoutService,
            FareMatrixService fareMatrixService) {

        this.seatRepository = seatRepository;
        this.showSeatRepository = showSeatRepository;
        this.showtimeLayoutService = showtimeLayoutService;
        this.fareMatrixService = fareMatrixService;
    }

    @Transactional
    public void validateForDeferredGeneration(Showtime showtime) {
        validateShowtime(showtime);
        loadSnapshots(showtime);
    }

    @Transactional
    public void validateCompleteGeneration(Showtime showtime, List<ShowSeat> showSeats) {

        validateScheduledShowtime(showtime);

        List<LayoutSeatSnapshot> snapshots = loadSnapshots(showtime);

        if (showSeats == null || showSeats.size() != snapshots.size()) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_LAYOUT_SEATS_MISMATCH);
        }

        Map<UUID, LayoutSeatSnapshot> expectedBySeatId = new HashMap<>();

        for (LayoutSeatSnapshot snapshot : snapshots) {
            UUID seatId = snapshot.seat().getId();

            if (expectedBySeatId.putIfAbsent(seatId, snapshot) != null) {
                throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
            }
        }

        Set<UUID> actualSeatIds = new HashSet<>();

        for (ShowSeat showSeat : showSeats) {
            if (showSeat == null
                    || showSeat.getSeat() == null
                    || showSeat.getSeat().getId() == null
                    || showSeat.getShowtime() == null
                    || !showtime.getId().equals(showSeat.getShowtime().getId())) {

                throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
            }

            UUID seatId = showSeat.getSeat().getId();
            LayoutSeatSnapshot expected = expectedBySeatId.get(seatId);

            if (expected == null || !actualSeatIds.add(seatId)) {
                throw new ConflictException(InventoryErrorCode.SHOWTIME_LAYOUT_SEATS_MISMATCH);
            }

            if (!Objects.equals(showSeat.getSeatNumber(), expected.seatNumber())
                    || showSeat.getSeatType() != expected.seatType()) {

                throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
            }
        }
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
