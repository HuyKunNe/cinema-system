package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse.ShowtimeSeatMapElementResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse.ShowtimeSeatMapSeatResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutElement;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutElementRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ShowtimeSeatMapService {

    private final ShowtimeRepository showtimeRepository;
    private final RoomLayoutSeatRepository layoutSeatRepository;
    private final RoomLayoutElementRepository layoutElementRepository;
    private final ShowSeatRepository showSeatRepository;
    private final Clock clock;

    public ShowtimeSeatMapService(
            ShowtimeRepository showtimeRepository,
            RoomLayoutSeatRepository layoutSeatRepository,
            RoomLayoutElementRepository layoutElementRepository,
            ShowSeatRepository showSeatRepository,
            Clock clock) {

        this.showtimeRepository = showtimeRepository;
        this.layoutSeatRepository = layoutSeatRepository;
        this.layoutElementRepository = layoutElementRepository;
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ShowtimeSeatMapResponse getSeatMap(UUID showtimeId) {

        Showtime showtime =
                showtimeRepository
                        .findByIdForSeatMap(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        RoomLayout layout = showtime.getRoomLayout();

        if (layout == null) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_LAYOUT_REQUIRED);
        }

        UUID roomId = showtime.getRoom().getId();

        requireConsistent(roomId.equals(layout.getRoom().getId()));

        if (layout.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
        }

        List<RoomLayoutSeat> positions =
                layoutSeatRepository.findAllForSeatMapByLayoutId(layout.getId());

        List<RoomLayoutElement> elements =
                layoutElementRepository.findAllByLayout_IdOrderByIdAsc(layout.getId());

        requireConsistent(
                !positions.isEmpty()
                        && elements.stream()
                                .anyMatch(
                                        element ->
                                                element.getKind() == RoomLayoutElementKind.SCREEN));

        Map<UUID, RoomLayoutSeat> positionsBySeatId = new HashMap<>();

        for (RoomLayoutSeat position : positions) {
            UUID seatId = position.getSeat().getId();

            requireConsistent(
                    roomId.equals(position.getSeat().getRoom().getId())
                            && position.getGeometry() != null
                            && position.getSeatTypeSnapshot() != null
                            && hasText(position.getSeatNumberSnapshot())
                            && hasText(position.getRowLabelSnapshot()));

            requireConsistent(positionsBySeatId.putIfAbsent(seatId, position) == null);
        }

        Map<UUID, ShowSeat> showSeatsBySeatId = new HashMap<>();

        for (ShowSeat showSeat : showSeatRepository.findAllForSeatMapByShowtimeId(showtimeId)) {

            UUID seatId = showSeat.getSeat().getId();
            RoomLayoutSeat position = positionsBySeatId.get(seatId);

            requireConsistent(
                    position != null
                            && roomId.equals(showSeat.getSeat().getRoom().getId())
                            && Objects.equals(
                                    showSeat.getSeatNumber(), position.getSeatNumberSnapshot())
                            && showSeat.getSeatType() == position.getSeatTypeSnapshot()
                            && showSeat.getPrice() != null
                            && showSeat.getPrice().signum() >= 0
                            && showSeat.getStatus() != null);

            requireConsistent(showSeatsBySeatId.putIfAbsent(seatId, showSeat) == null);
        }

        OffsetDateTime serverTime = OffsetDateTime.now(clock);

        boolean bookable =
                showtimeRepository.existsHoldEligibleShowtime(
                        showtimeId, serverTime, ShowtimeStatus.OPEN_FOR_BOOKING);

        List<ShowtimeSeatMapSeatResponse> seats = new ArrayList<>();

        for (RoomLayoutSeat position : positions) {
            seats.add(
                    toSeatResponse(
                            position, showSeatsBySeatId.get(position.getSeat().getId()), bookable));
        }

        List<ShowtimeSeatMapElementResponse> elementResponses =
                elements.stream().map(this::toElementResponse).toList();

        return new ShowtimeSeatMapResponse(
                showtime.getId(),
                showtime.getRoom().getCinema().getId(),
                roomId,
                layout.getId(),
                layout.getLayoutVersion(),
                serverTime,
                layout.getCanvasWidth(),
                layout.getCanvasHeight(),
                elementResponses,
                List.copyOf(seats));
    }

    private ShowtimeSeatMapSeatResponse toSeatResponse(
            RoomLayoutSeat position, ShowSeat showSeat, boolean bookable) {

        var geometry = position.getGeometry();
        var seatType = position.getSeatTypeSnapshot();

        boolean selectable =
                bookable && showSeat != null && showSeat.getStatus() == ShowSeatStatus.AVAILABLE;

        return new ShowtimeSeatMapSeatResponse(
                position.getSeat().getId(),
                showSeat == null ? null : showSeat.getId(),
                position.getSeatNumberSnapshot(),
                position.getRowLabelSnapshot(),
                seatType,
                seatType.getCapacity(),
                geometry.getX(),
                geometry.getY(),
                geometry.getWidth(),
                geometry.getHeight(),
                geometry.getRotationDegrees(),
                showSeat == null ? null : showSeat.getPrice(),
                InventoryEventContract.CURRENCY_VND,
                showSeat == null ? null : showSeat.getStatus(),
                selectable);
    }

    private ShowtimeSeatMapElementResponse toElementResponse(RoomLayoutElement element) {

        requireConsistent(element.getKind() != null && element.getGeometry() != null);

        var geometry = element.getGeometry();

        return new ShowtimeSeatMapElementResponse(
                element.getId(),
                element.getKind(),
                element.getLabel(),
                geometry.getX(),
                geometry.getY(),
                geometry.getWidth(),
                geometry.getHeight(),
                geometry.getRotationDegrees());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void requireConsistent(boolean condition) {
        if (!condition) {
            throw new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
        }
    }
}
