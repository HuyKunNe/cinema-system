package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceShowtimePricingRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicyVersion;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.PricingTimeBand;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.enums.TicketCategory;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShowtimePricingService {

    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final CinemaPricingPolicyVersionRepository versionRepository;
    private final ShowSeatRepository showSeatRepository;
    private final FareMatrixService fareMatrixService;
    private final ShowtimeMapper mapper;

    public ShowtimePricingService(
            ShowtimeRepository showtimeRepository,
            RoomRepository roomRepository,
            CinemaRepository cinemaRepository,
            CinemaPricingPolicyVersionRepository versionRepository,
            ShowSeatRepository showSeatRepository,
            FareMatrixService fareMatrixService,
            ShowtimeMapper mapper) {

        this.showtimeRepository = showtimeRepository;
        this.roomRepository = roomRepository;
        this.cinemaRepository = cinemaRepository;
        this.versionRepository = versionRepository;
        this.showSeatRepository = showSeatRepository;
        this.fareMatrixService = fareMatrixService;
        this.mapper = mapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse replace(UUID showtimeId, ReplaceShowtimePricingRequest request) {

        if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0) {

            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PRICING);
        }

        Showtime showtime = lockShowtime(showtimeId);

        if (!request.expectedVersion().equals(showtime.getVersion())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_VERSION_CONFLICT);
        }

        requireScheduled(showtime);

        List<ShowSeat> seats = lockSeats(showtime);

        CinemaPricingPolicyVersion selected = null;

        if (request.pricingPolicyVersionId() != null) {
            selected =
                    versionRepository
                            .findById(request.pricingPolicyVersionId())
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    InventoryErrorCode
                                                            .PRICING_POLICY_VERSION_NOT_FOUND));
        }

        showtime.replacePricingPolicyVersion(selected);

        if (selected != null) {
            applyFares(showtime, seats);
        }

        showtimeRepository.flush();

        return mapper.toResponse(showtime);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void refreshIfAssigned(Showtime showtime) {
        requireScheduled(showtime);

        if (showtime.getPricingPolicyVersion() != null) {
            applyFares(showtime, lockSeats(showtime));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void requireAssignedAndRefresh(Showtime showtime) {
        requireScheduled(showtime);

        if (showtime.getPricingPolicyVersion() == null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_REQUIRED);
        }

        applyFares(showtime, lockSeats(showtime));
    }

    private Showtime lockShowtime(UUID showtimeId) {
        UUID roomId =
                showtimeRepository
                        .findRoomIdForScheduleChange(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        var room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        cinemaRepository
                .findByIdForUpdate(room.getCinema().getId())
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        Showtime showtime =
                showtimeRepository
                        .findByIdForUpdate(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        if (!roomId.equals(showtime.getRoom().getId())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_VERSION_CONFLICT);
        }

        return showtime;
    }

    private List<ShowSeat> lockSeats(Showtime showtime) {
        List<UUID> ids = showSeatRepository.findIdsByShowtimeId(showtime.getId());

        if (ids.isEmpty()) {
            return List.of();
        }

        List<ShowSeat> seats =
                showSeatRepository.findAllByShowtimeIdAndIdsForUpdate(showtime.getId(), ids);

        if (seats.size() != ids.size()) {
            throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
        }

        for (ShowSeat seat : seats) {
            if (seat.getStatus() == ShowSeatStatus.HELD) {
                throw new ConflictException(InventoryErrorCode.HELD_SHOW_SEAT_CANNOT_BE_CHANGED);
            }

            if (seat.getStatus() == ShowSeatStatus.BOOKED) {
                throw new ConflictException(InventoryErrorCode.BOOKED_SHOW_SEAT_CANNOT_BE_CHANGED);
            }
        }

        return seats;
    }

    private void applyFares(Showtime showtime, List<ShowSeat> seats) {

        if (seats.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.NO_ACTIVE_SEATS);
        }

        var selected = showtime.getPricingPolicyVersion();

        // Recheck publication and cinema applicability.
        showtime.replacePricingPolicyVersion(selected);

        if (showtime.getRoom().getRoomType() == null
                || showtime.getScreeningVersion() == null
                || showtime.getScreeningVersion().getContentFormat() == null) {

            throw new ConflictException(InventoryErrorCode.SHOWTIME_PRICING_CONTEXT_REQUIRED);
        }

        var matrix =
                fareMatrixService.getForShowtimePricing(
                        showtime.getRoom().getCinema().getId(),
                        selected.getPolicy().getId(),
                        selected.getId());

        if (matrix.status() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED);
        }

        if (!matrix.coverageComplete()) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
        }

        var experience = showtime.getRoom().getRoomType().getPricingExperienceType();

        var format = showtime.getScreeningVersion().getContentFormat();

        PricingTimeBand timeBand = resolveTimeBand(showtime);

        Map<SeatType, BigDecimal> prices = new EnumMap<>(SeatType.class);

        for (ShowSeat seat : seats) {
            SeatType seatType = seat.getSeatType();

            if (prices.containsKey(seatType)) {
                continue;
            }

            var fare =
                    matrix.cells().stream()
                            .filter(
                                    cell ->
                                            cell.experienceType() == experience
                                                    && cell.contentFormat() == format
                                                    && cell.timeBand() == timeBand
                                                    && cell.ticketCategory() == TicketCategory.ADULT
                                                    && cell.seatType() == seatType)
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new ConflictException(
                                                    InventoryErrorCode
                                                            .PRICING_FARE_NOT_APPLICABLE));

            if (fare.state() != FareCellState.PRICED) {
                throw new ConflictException(InventoryErrorCode.PRICING_FARE_NOT_APPLICABLE);
            }

            prices.put(seatType, fare.amount());
        }

        for (ShowSeat seat : seats) {
            seat.changePrice(prices.get(seat.getSeatType()));
        }
    }

    private static PricingTimeBand resolveTimeBand(Showtime showtime) {
        String configured = showtime.getRoom().getCinema().getTimeZone();

        String zoneName = configured == null ? "Asia/Ho_Chi_Minh" : configured;

        if (!ZoneId.getAvailableZoneIds().contains(zoneName)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        return PricingTimeBand.fromLocalTime(
                showtime.getStartsAt().atZoneSameInstant(ZoneId.of(zoneName)).toLocalTime());
    }

    private static void requireScheduled(Showtime showtime) {
        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }
    }
}
