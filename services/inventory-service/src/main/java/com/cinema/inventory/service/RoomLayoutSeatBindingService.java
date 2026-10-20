package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.SeatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomLayoutSeatBindingService {

    private final RoomLayoutMatrixCellRepository cellRepository;
    private final SeatRepository seatRepository;
    private final RoomLayoutMatrixValidator validator;

    public RoomLayoutSeatBindingService(
            RoomLayoutMatrixCellRepository cellRepository,
            SeatRepository seatRepository,
            RoomLayoutMatrixValidator validator) {

        this.cellRepository = cellRepository;
        this.seatRepository = seatRepository;
        this.validator = validator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void bindForPublication(RoomLayout layout) {
        layout.requireDraft();

        if (!layout.isMatrixLayout()) {
            throw invalid();
        }

        List<RoomLayoutMatrixCell> cells =
                cellRepository.findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(layout.getId());

        if (cells.isEmpty()) {
            throw new ValidationException(InventoryErrorCode.ROOM_LAYOUT_MATRIX_EMPTY);
        }

        List<MatrixSeatInput> inputs = new ArrayList<>();
        List<UUID> existingSeatIds = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            if (cell.getSeatType() == null) {
                throw invalid();
            }

            UUID seatId = null;

            if (cell.getSeat() != null) {
                seatId = cell.getSeat().getId();

                if (seatId == null) {
                    throw invalid();
                }

                existingSeatIds.add(seatId);
            }

            inputs.add(
                    new MatrixSeatInput(
                            seatId,
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            cell.getNumber(),
                            cell.getSeatType()));
        }

        validator.validate(
                new RoomLayoutMatrixInput(layout.getRowCount(), layout.getColumnCount(), inputs));

        Map<UUID, Seat> managedSeats = new HashMap<>();

        if (!existingSeatIds.isEmpty()) {
            for (Seat seat :
                    seatRepository.findAllForLayoutGeneration(
                            layout.getRoom().getId(), existingSeatIds)) {
                managedSeats.put(seat.getId(), seat);
            }
        }

        // Every supplied identity must still exist in the same room.
        for (UUID seatId : existingSeatIds) {
            if (!managedSeats.containsKey(seatId)) {
                throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH);
            }
        }

        List<Seat> newSeats = new ArrayList<>();
        Map<RoomLayoutMatrixCell, UUID> bindings = new LinkedHashMap<>();

        for (RoomLayoutMatrixCell cell : cells) {
            UUID seatId;

            if (cell.getSeat() != null) {
                seatId = cell.getSeat().getId();
            } else {
                String rowLabel = validator.rowLabel(cell.getRowIndex());

                Seat seat =
                        Seat.createForLayout(
                                layout.getRoom(),
                                rowLabel + cell.getNumber(),
                                rowLabel,
                                cell.getSeatType());

                newSeats.add(seat);
                seatId = seat.getId();
            }

            bindings.put(cell, seatId);
        }

        if (!newSeats.isEmpty()) {
            for (Seat savedSeat : seatRepository.saveAll(newSeats)) {
                managedSeats.put(savedSeat.getId(), savedSeat);
            }
        }

        for (Map.Entry<RoomLayoutMatrixCell, UUID> binding : bindings.entrySet()) {

            Seat seat = managedSeats.get(binding.getValue());

            if (seat == null) {
                throw invalid();
            }

            binding.getKey().bindSeat(seat);
        }

        // Flush while the layout is still DRAFT.
        // Any later publication failure rolls back these writes as well.
        cellRepository.flush();
    }

    private ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
    }
}
