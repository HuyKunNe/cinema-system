package com.cinema.inventory.service;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse;
import com.cinema.inventory.dto.response.RoomLayoutMatrixResponse;
import com.cinema.inventory.dto.response.RoomLayoutMatrixResponse.MatrixSeatResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.SeatRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomLayoutMatrixService {

    private final RoomLayoutMatrixCellRepository cellRepository;
    private final RoomLayoutRepository layoutRepository;
    private final SeatRepository seatRepository;
    private final RoomLayoutMatrixValidator validator;
    private final RoomLayoutMapper mapper;

    public RoomLayoutMatrixService(
            RoomLayoutMatrixCellRepository cellRepository,
            RoomLayoutRepository layoutRepository,
            SeatRepository seatRepository,
            RoomLayoutMatrixValidator validator,
            RoomLayoutMapper mapper) {

        this.cellRepository = cellRepository;
        this.layoutRepository = layoutRepository;
        this.seatRepository = seatRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public RoomLayoutContentResponse read(RoomLayout layout) {
        List<MatrixSeatResponse> seats =
                cellRepository
                        .findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(
                                layout.getId())
                        .stream()
                        .map(cell -> new MatrixSeatResponse(
                                cell.getSeat() == null
                                        ? null
                                        : cell.getSeat().getId(),
                                cell.getRowIndex(),
                                cell.getColumnIndex(),
                                cell.getNumber(),
                                cell.getSeatType()))
                        .toList();

        RoomLayoutMatrixResponse matrix = new RoomLayoutMatrixResponse(
                layout.getRowCount(),
                layout.getColumnCount(),
                seats);

        return new RoomLayoutContentResponse(
                mapper.toResponse(layout),
                List.of(),
                List.of(),
                matrix);
    }

    @Transactional
    public RoomLayoutContentResponse replace(
            RoomLayout layout,
            RoomLayoutMatrixInput input) {

        layout.requireDraft();
        validator.validate(input);

        List<UUID> seatIds = input.seats().stream()
                .map(RoomLayoutMatrixInput.MatrixSeatInput::seatId)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, Seat> seatsById = seatRepository.findAllById(seatIds)
                .stream()
                .collect(Collectors.toMap(
                        Seat::getId,
                        Function.identity()));

        for (UUID seatId : seatIds) {
            if (!seatsById.containsKey(seatId)) {
                throw new NotFoundException(
                        InventoryErrorCode.SEAT_NOT_FOUND);
            }
        }

        layout.defineMatrix(input.rowCount(), input.columnCount());

        List<RoomLayoutMatrixCell> cells = input.seats().stream()
                .map(seat -> new RoomLayoutMatrixCell(
                        layout,
                        seat.seatId() == null
                                ? null
                                : seatsById.get(seat.seatId()),
                        seat.rowIndex(),
                        seat.columnIndex(),
                        seat.number(),
                        seat.seatType()))
                .toList();

        // Validate and construct every cell before replacing stored content.
        // RoomLayoutMatrixCell also checks that linked seats belong to this room.
        layout.markContentChanged();

        cellRepository.deleteAllForLayout(layout.getId());
        cellRepository.saveAllAndFlush(cells);
        layoutRepository.flush();

        return read(layout);
    }
}
