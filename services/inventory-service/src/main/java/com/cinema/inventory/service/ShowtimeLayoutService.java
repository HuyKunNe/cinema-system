package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.SeatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowtimeLayoutService {

    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutSeatRepository layoutSeatRepository;
    private final SeatRepository seatRepository;
    private final RoomLayoutMatrixCellRepository matrixCellRepository;
    private final RoomLayoutMatrixValidator matrixValidator;

    public ShowtimeLayoutService(
            RoomLayoutRepository layoutRepository,
            RoomLayoutSeatRepository layoutSeatRepository,
            SeatRepository seatRepository,
            RoomLayoutMatrixCellRepository matrixCellRepository,
            RoomLayoutMatrixValidator matrixValidator) {

        this.layoutRepository = layoutRepository;
        this.layoutSeatRepository = layoutSeatRepository;
        this.seatRepository = seatRepository;
        this.matrixCellRepository = matrixCellRepository;
        this.matrixValidator = matrixValidator;
    }

    @Transactional
    public RoomLayout findPublishedLayout(UUID layoutId, UUID roomId) {
        RoomLayout layout =
                layoutRepository
                        .findByIdForRead(layoutId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        if (!layout.getRoom().getId().equals(roomId)) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_LAYOUT_ROOM_MISMATCH);
        }

        if (layout.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
        }

        return layout;
    }

    @Transactional
    public List<LayoutSeatSnapshot> getSeatSnapshotsForGeneration(Showtime showtime) {

        if (showtime == null || showtime.getRoomLayout() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_LAYOUT_REQUIRED);
        }

        if (showtime.getRoom() == null || showtime.getRoom().getId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }

        UUID roomId = showtime.getRoom().getId();

        RoomLayout layout = findPublishedLayout(showtime.getRoomLayout().getId(), roomId);

        if (layout.isMatrixLayout()) {
            return getMatrixSnapshots(layout, roomId);
        }

        return getLegacySnapshots(layout, roomId);
    }

    private List<LayoutSeatSnapshot> getMatrixSnapshots(RoomLayout layout, UUID roomId) {

        List<RoomLayoutMatrixCell> cells =
                matrixCellRepository.findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(
                        layout.getId());

        if (cells.isEmpty()) {
            throw invalidLayout();
        }

        List<MatrixSeatInput> inputs = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            if (cell.getSeat() == null
                    || cell.getSeat().getId() == null
                    || cell.getSeatType() == null) {
                throw invalidLayout();
            }

            inputs.add(
                    new MatrixSeatInput(
                            cell.getSeat().getId(),
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            cell.getNumber(),
                            cell.getSeatType()));
        }

        matrixValidator.validate(
                new RoomLayoutMatrixInput(layout.getRowCount(), layout.getColumnCount(), inputs));

        List<UUID> seatIds = inputs.stream().map(MatrixSeatInput::seatId).toList();

        Map<UUID, Seat> seatsById = findSeats(roomId, seatIds);
        List<LayoutSeatSnapshot> snapshots = new ArrayList<>();

        for (MatrixSeatInput input : inputs) {
            Seat seat = requireSeat(seatsById, input.seatId());

            String seatNumber = matrixValidator.rowLabel(input.rowIndex()) + input.number();

            snapshots.add(new LayoutSeatSnapshot(seat, seatNumber, input.seatType()));
        }

        return validateSnapshots(snapshots);
    }

    private List<LayoutSeatSnapshot> getLegacySnapshots(RoomLayout layout, UUID roomId) {

        List<RoomLayoutSeat> positions =
                layoutSeatRepository.findPositionsForGenerationByLayoutId(layout.getId());

        if (positions.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_INCOMPLETE);
        }

        List<UUID> seatIds = new ArrayList<>();

        for (RoomLayoutSeat position : positions) {
            if (position.getSeat() == null || position.getSeat().getId() == null) {
                throw invalidLayout();
            }

            seatIds.add(position.getSeat().getId());
        }

        Map<UUID, Seat> seatsById = findSeats(roomId, seatIds);
        List<LayoutSeatSnapshot> snapshots = new ArrayList<>();

        for (RoomLayoutSeat position : positions) {
            Seat seat = requireSeat(seatsById, position.getSeat().getId());

            // Preserve the existing legacy-layout compatibility check.
            if (!Objects.equals(seat.getSeatNumber(), position.getSeatNumberSnapshot())
                    || !Objects.equals(seat.getRowLabel(), position.getRowLabelSnapshot())
                    || seat.getSeatType() != position.getSeatTypeSnapshot()) {
                throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH);
            }

            snapshots.add(
                    new LayoutSeatSnapshot(
                            seat,
                            position.getSeatNumberSnapshot(),
                            position.getSeatTypeSnapshot()));
        }

        return validateSnapshots(snapshots);
    }

    private Map<UUID, Seat> findSeats(UUID roomId, List<UUID> seatIds) {

        Map<UUID, Seat> seatsById = new HashMap<>();

        for (Seat seat : seatRepository.findAllForLayoutGeneration(roomId, seatIds)) {
            seatsById.put(seat.getId(), seat);
        }

        return seatsById;
    }

    private Seat requireSeat(Map<UUID, Seat> seatsById, UUID seatId) {

        Seat seat = seatsById.get(seatId);

        if (seat == null) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH);
        }

        return seat;
    }

    private List<LayoutSeatSnapshot> validateSnapshots(List<LayoutSeatSnapshot> snapshots) {

        Set<UUID> seatIds = new HashSet<>();
        Set<String> seatNumbers = new HashSet<>();

        for (LayoutSeatSnapshot snapshot : snapshots) {
            if (!seatIds.add(snapshot.seat().getId()) || !seatNumbers.add(snapshot.seatNumber())) {
                throw invalidLayout();
            }
        }

        return List.copyOf(snapshots);
    }

    private ValidationException invalidLayout() {
        return new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
    }
}
