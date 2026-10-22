package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse.ShowtimeSeatMapElementResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse.ShowtimeSeatMapMatrixResponse;
import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse.ShowtimeSeatMapSeatResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutElement;
import com.cinema.inventory.entity.RoomLayoutGeometry;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomLayoutElementKind;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutElementRepository;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
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
    private final RoomLayoutMatrixCellRepository matrixCellRepository;
    private final RoomLayoutMatrixValidator matrixValidator;

    public ShowtimeSeatMapService(
            ShowtimeRepository showtimeRepository,
            RoomLayoutSeatRepository layoutSeatRepository,
            RoomLayoutElementRepository layoutElementRepository,
            ShowSeatRepository showSeatRepository,
            Clock clock,
            RoomLayoutMatrixCellRepository matrixCellRepository,
            RoomLayoutMatrixValidator matrixValidator) {

        this.showtimeRepository = showtimeRepository;
        this.layoutSeatRepository = layoutSeatRepository;
        this.layoutElementRepository = layoutElementRepository;
        this.showSeatRepository = showSeatRepository;
        this.clock = clock;
        this.matrixCellRepository = matrixCellRepository;
        this.matrixValidator = matrixValidator;
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

        // A stored layout cannot contain only one matrix dimension.
        requireConsistent((layout.getRowCount() == null) == (layout.getColumnCount() == null));

        OffsetDateTime serverTime = OffsetDateTime.now(clock);

        boolean bookable =
                showtimeRepository.existsHoldEligibleShowtime(
                        showtimeId, serverTime, ShowtimeStatus.OPEN_FOR_BOOKING);

        if (layout.isMatrixLayout()) {
            return getMatrixSeatMap(showtime, layout, serverTime, bookable);
        }

        return getLegacySeatMap(showtime, layout, serverTime, bookable);
    }

    private ShowtimeSeatMapResponse getMatrixSeatMap(
            Showtime showtime, RoomLayout layout, OffsetDateTime serverTime, boolean bookable) {

        UUID roomId = showtime.getRoom().getId();

        List<RoomLayoutMatrixCell> cells =
                matrixCellRepository.findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(
                        layout.getId());

        requireConsistent(!cells.isEmpty());

        List<MatrixSeatInput> inputs = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            requireConsistent(
                    cell.getSeat() != null
                            && cell.getSeat().getId() != null
                            && cell.getSeat().getRoom() != null
                            && roomId.equals(cell.getSeat().getRoom().getId())
                            && cell.getSeatType() != null);

            inputs.add(
                    new MatrixSeatInput(
                            cell.getSeat().getId(),
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            cell.getNumber(),
                            cell.getSeatType()));
        }

        try {
            matrixValidator.validate(
                    new RoomLayoutMatrixInput(
                            layout.getRowCount(), layout.getColumnCount(), inputs));
        } catch (ValidationException exception) {
            // Invalid persisted content is a consistency conflict,
            // not invalid input supplied to this GET endpoint.
            throw inconsistent();
        }

        Map<UUID, SeatSnapshot> snapshots = new HashMap<>();

        for (RoomLayoutMatrixCell cell : cells) {
            String rowLabel = matrixValidator.rowLabel(cell.getRowIndex());

            SeatSnapshot snapshot =
                    new SeatSnapshot(rowLabel + cell.getNumber(), rowLabel, cell.getSeatType());

            requireConsistent(snapshots.putIfAbsent(cell.getSeat().getId(), snapshot) == null);
        }

        Map<UUID, ShowSeat> showSeats = readShowSeats(showtime.getId(), roomId, snapshots);

        List<ShowtimeSeatMapSeatResponse> seats = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            UUID seatId = cell.getSeat().getId();

            seats.add(
                    toSeatResponse(
                            seatId,
                            snapshots.get(seatId),
                            null,
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            cell.getNumber(),
                            cell.getSeatType().getMatrixColumnSpan(),
                            showSeats.get(seatId),
                            bookable));
        }

        return new ShowtimeSeatMapResponse(
                showtime.getId(),
                showtime.getRoom().getCinema().getId(),
                roomId,
                layout.getId(),
                layout.getLayoutVersion(),
                serverTime,
                null,
                null,
                List.of(),
                List.copyOf(seats),
                new ShowtimeSeatMapMatrixResponse(layout.getRowCount(), layout.getColumnCount()));
    }

    private ShowtimeSeatMapResponse getLegacySeatMap(
            Showtime showtime, RoomLayout layout, OffsetDateTime serverTime, boolean bookable) {

        UUID roomId = showtime.getRoom().getId();

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

        Map<UUID, SeatSnapshot> snapshots = new HashMap<>();

        for (RoomLayoutSeat position : positions) {
            requireConsistent(
                    position.getSeat() != null
                            && position.getSeat().getId() != null
                            && position.getSeat().getRoom() != null
                            && roomId.equals(position.getSeat().getRoom().getId())
                            && position.getGeometry() != null
                            && position.getSeatTypeSnapshot() != null
                            && hasText(position.getSeatNumberSnapshot())
                            && hasText(position.getRowLabelSnapshot()));

            SeatSnapshot snapshot =
                    new SeatSnapshot(
                            position.getSeatNumberSnapshot(),
                            position.getRowLabelSnapshot(),
                            position.getSeatTypeSnapshot());

            requireConsistent(snapshots.putIfAbsent(position.getSeat().getId(), snapshot) == null);
        }

        Map<UUID, ShowSeat> showSeats = readShowSeats(showtime.getId(), roomId, snapshots);

        List<ShowtimeSeatMapSeatResponse> seats = new ArrayList<>();

        for (RoomLayoutSeat position : positions) {
            UUID seatId = position.getSeat().getId();

            seats.add(
                    toSeatResponse(
                            seatId,
                            snapshots.get(seatId),
                            position.getGeometry(),
                            null,
                            null,
                            null,
                            null,
                            showSeats.get(seatId),
                            bookable));
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
                List.copyOf(seats),
                null);
    }

    private Map<UUID, ShowSeat> readShowSeats(
            UUID showtimeId, UUID roomId, Map<UUID, SeatSnapshot> snapshots) {

        Map<UUID, ShowSeat> result = new HashMap<>();

        for (ShowSeat showSeat : showSeatRepository.findAllForSeatMapByShowtimeId(showtimeId)) {

            requireConsistent(
                    showSeat.getSeat() != null
                            && showSeat.getSeat().getId() != null
                            && showSeat.getSeat().getRoom() != null);

            UUID seatId = showSeat.getSeat().getId();
            SeatSnapshot snapshot = snapshots.get(seatId);

            requireConsistent(
                    snapshot != null
                            && roomId.equals(showSeat.getSeat().getRoom().getId())
                            && Objects.equals(showSeat.getSeatNumber(), snapshot.seatNumber())
                            && showSeat.getSeatType() == snapshot.seatType()
                            && (showSeat.getShowtime().usesPolicyPricing()
                                    ? showSeat.getPrice() == null
                                    : showSeat.getPrice() != null
                                            && showSeat.getPrice().signum() >= 0)
                            && showSeat.getStatus() != null);

            requireConsistent(result.putIfAbsent(seatId, showSeat) == null);
        }

        return result;
    }

    private ShowtimeSeatMapSeatResponse toSeatResponse(
            UUID seatId,
            SeatSnapshot snapshot,
            RoomLayoutGeometry geometry,
            Integer rowIndex,
            Integer columnIndex,
            Integer number,
            Integer columnSpan,
            ShowSeat showSeat,
            boolean bookable) {

        boolean selectable =
                bookable && showSeat != null && showSeat.getStatus() == ShowSeatStatus.AVAILABLE;

        return new ShowtimeSeatMapSeatResponse(
                seatId,
                showSeat == null ? null : showSeat.getId(),
                snapshot.seatNumber(),
                snapshot.rowLabel(),
                snapshot.seatType(),
                snapshot.seatType().getCapacity(),
                geometry == null ? null : geometry.getX(),
                geometry == null ? null : geometry.getY(),
                geometry == null ? null : geometry.getWidth(),
                geometry == null ? null : geometry.getHeight(),
                geometry == null ? null : geometry.getRotationDegrees(),
                showSeat == null ? null : showSeat.getPrice(),
                InventoryEventContract.CURRENCY_VND,
                showSeat == null ? null : showSeat.getStatus(),
                selectable,
                rowIndex,
                columnIndex,
                number,
                columnSpan);
    }

    private ShowtimeSeatMapElementResponse toElementResponse(RoomLayoutElement element) {

        requireConsistent(element.getKind() != null && element.getGeometry() != null);

        RoomLayoutGeometry geometry = element.getGeometry();

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
            throw inconsistent();
        }
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }

    private record SeatSnapshot(String seatNumber, String rowLabel, SeatType seatType) {}
}
