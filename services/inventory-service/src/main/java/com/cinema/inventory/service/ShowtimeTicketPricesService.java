package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.CellResponse;
import com.cinema.inventory.dto.response.FareMatrixResponse.TicketGroupResponse;
import com.cinema.inventory.dto.response.ShowtimeTicketPricesResponse;
import com.cinema.inventory.dto.response.ShowtimeTicketPricesResponse.SeatFare;
import com.cinema.inventory.dto.response.ShowtimeTicketPricesResponse.TicketGroupPrices;
import com.cinema.inventory.enums.FareCellState;
import com.cinema.inventory.enums.FareUnit;
import com.cinema.inventory.enums.PricingPolicyVersionStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ShowtimeTicketPricesService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;
    private final FareMatrixService fareMatrixService;
    private final Clock clock;

    public ShowtimeTicketPricesService(
            ShowtimeRepository showtimeRepository,
            ShowSeatRepository showSeatRepository,
            FareMatrixService fareMatrixService,
            Clock clock) {

        this.showtimeRepository = showtimeRepository;
        this.showSeatRepository = showSeatRepository;
        this.fareMatrixService = fareMatrixService;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ShowtimeTicketPricesResponse get(UUID showtimeId) {

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

        FareMatrixResponse matrix =
                fareMatrixService.get(
                        cinema.getId(), selected.getPolicy().getId(), selected.getId());

        requirePublishedMatrix(matrix);

        var zone = PricingFareResolver.cinemaZone(cinema.getTimeZone());

        var localStartsAt = showtime.getStartsAt().atZoneSameInstant(zone).toOffsetDateTime();

        var timeBand = PricingFareResolver.resolveTimeBand(matrix, localStartsAt.toLocalTime());

        var experience = room.getRoomType().getPricingExperienceType();
        var format = screening.getContentFormat();

        List<SeatType> seatTypes = showSeatRepository.findSeatTypesForTicketPrices(showtimeId);

        if (seatTypes.isEmpty() || seatTypes.stream().anyMatch(type -> type == null)) {

            throw inconsistent();
        }

        seatTypes =
                seatTypes.stream()
                        .distinct()
                        .sorted(Comparator.comparingInt(SeatType::ordinal))
                        .toList();

        Map<FareKey, CellResponse> fares = new HashMap<>();

        for (CellResponse cell : matrix.cells()) {
            if (cell.experienceType() != experience
                    || cell.contentFormat() != format
                    || !timeBand.id().equals(cell.timeBandId())
                    || !seatTypes.contains(cell.seatType())) {

                continue;
            }

            FareKey key = new FareKey(cell.ticketGroupId(), cell.seatType());

            if (fares.putIfAbsent(key, cell) != null) {
                throw inconsistent();
            }
        }

        List<TicketGroupResponse> groups =
                matrix.ticketGroups().stream()
                        .sorted(
                                Comparator.comparingInt(TicketGroupResponse::sortOrder)
                                        .thenComparing(TicketGroupResponse::id))
                        .toList();

        List<TicketGroupPrices> result = new ArrayList<>();

        for (TicketGroupResponse group : groups) {
            List<SeatFare> groupFares = new ArrayList<>();

            for (SeatType seatType : seatTypes) {
                CellResponse cell = fares.get(new FareKey(group.id(), seatType));

                groupFares.add(toFare(seatType, cell));
            }

            result.add(
                    new TicketGroupPrices(
                            group.id(),
                            group.name(),
                            group.eligibilityDescription(),
                            group.defaultGroup(),
                            List.copyOf(groupFares)));
        }

        boolean bookable =
                showtimeRepository.existsHoldEligibleShowtime(
                        showtimeId, OffsetDateTime.now(clock), ShowtimeStatus.OPEN_FOR_BOOKING);

        if (!bookable) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_BOOKABLE);
        }

        return new ShowtimeTicketPricesResponse(
                showtimeId,
                cinema.getId(),
                matrix.policyVersionId(),
                matrix.revision(),
                matrix.dayGroup(),
                experience,
                format,
                InventoryEventContract.CURRENCY_VND,
                zone.getId(),
                localStartsAt,
                timeBand.id(),
                timeBand.name(),
                matrix.defaultTicketGroupId(),
                List.copyOf(result));
    }

    private static void requirePublishedMatrix(FareMatrixResponse matrix) {

        if (matrix.status() != PricingPolicyVersionStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.PRICING_POLICY_VERSION_NOT_PUBLISHED);
        }

        long defaultCount =
                matrix.ticketGroups().stream().filter(TicketGroupResponse::defaultGroup).count();

        boolean hasDefault =
                matrix.ticketGroups().stream()
                        .anyMatch(
                                group ->
                                        group.defaultGroup()
                                                && group.id()
                                                        .equals(matrix.defaultTicketGroupId()));

        if (!matrix.coverageComplete() || defaultCount != 1 || !hasDefault) {

            throw inconsistent();
        }
    }

    private static SeatFare toFare(SeatType seatType, CellResponse cell) {

        FareUnit unit = seatType == SeatType.COUPLE ? FareUnit.PAIR : FareUnit.SEAT;

        if (cell == null || cell.unit() != unit) {
            throw inconsistent();
        }

        if (cell.state() == FareCellState.NOT_APPLICABLE) {
            if (cell.amount() != null) {
                throw inconsistent();
            }

            return new SeatFare(seatType, seatType.getCapacity(), unit, false, null);
        }

        if (cell.state() != FareCellState.PRICED
                || cell.amount() == null
                || cell.amount().signum() < 0) {

            throw inconsistent();
        }

        return new SeatFare(seatType, seatType.getCapacity(), unit, true, cell.amount());
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.PRICING_POLICY_DATA_INCONSISTENT);
    }

    private record FareKey(UUID ticketGroupId, SeatType seatType) {}
}
