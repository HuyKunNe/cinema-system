package com.cinema.inventory.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.dto.response.RoomPublishedSeatMapResponse;
import com.cinema.inventory.dto.response.RoomPublishedSeatMapResponse.PublishedRoomMatrixResponse;
import com.cinema.inventory.dto.response.RoomPublishedSeatMapResponse.PublishedRoomSeatResponse;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomRepository;

@Service
public class RoomPublishedSeatMapService {

    private final RoomRepository roomRepository;
    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutMatrixCellRepository cellRepository;
    private final RoomLayoutMatrixValidator matrixValidator;
    private final RoomLayoutMapper layoutMapper;

    public RoomPublishedSeatMapService(
            RoomRepository roomRepository,
            RoomLayoutRepository layoutRepository,
            RoomLayoutMatrixCellRepository cellRepository,
            RoomLayoutMatrixValidator matrixValidator,
            RoomLayoutMapper layoutMapper) {

        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.cellRepository = cellRepository;
        this.matrixValidator = matrixValidator;
        this.layoutMapper = layoutMapper;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ)
    public RoomPublishedSeatMapResponse getSeatMap(
            UUID roomId,
            UUID layoutId) {

        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException(
                    InventoryErrorCode.ROOM_NOT_FOUND);
        }

        RoomLayout layout = findLayout(roomId, layoutId);

        if (layout.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
        }

        if (!layout.isMatrixLayout()) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_MATRIX_CONVERSION_REQUIRED);
        }

        List<RoomLayoutMatrixCell> cells = cellRepository
                .findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(
                        layout.getId());

        validateStoredMatrix(layout, roomId, cells);

        List<PublishedRoomSeatResponse> seats = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            String rowLabel = matrixValidator.rowLabel(
                    cell.getRowIndex());

            seats.add(new PublishedRoomSeatResponse(
                    cell.getSeat().getId(),
                    cell.getRowIndex(),
                    cell.getColumnIndex(),
                    cell.getNumber(),
                    rowLabel + cell.getNumber(),
                    rowLabel,
                    cell.getSeatType(),
                    cell.getSeatType().getCapacity(),
                    cell.getSeatType().getMatrixColumnSpan(),
                    cell.getSeat().isActive()));
        }

        return new RoomPublishedSeatMapResponse(
                layoutMapper.toResponse(layout),
                new PublishedRoomMatrixResponse(
                        layout.getRowCount(),
                        layout.getColumnCount(),
                        List.copyOf(seats)));
    }

    private RoomLayout findLayout(
            UUID roomId,
            UUID layoutId) {

        if (layoutId != null) {
            return layoutRepository
                    .findByIdAndRoom_Id(layoutId, roomId)
                    .orElseThrow(() -> new NotFoundException(
                            InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));
        }

        return layoutRepository
                .findFirstByRoom_IdAndStatusOrderByLayoutVersionDesc(
                        roomId,
                        RoomLayoutStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));
    }

    private void validateStoredMatrix(
            RoomLayout layout,
            UUID roomId,
            List<RoomLayoutMatrixCell> cells) {

        if (cells.isEmpty()) {
            throw inconsistent();
        }

        List<MatrixSeatInput> inputs = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            if (cell.getSeat() == null
                    || cell.getSeat().getId() == null
                    || cell.getSeat().getRoom() == null
                    || !roomId.equals(
                            cell.getSeat().getRoom().getId())
                    || cell.getSeatType() == null) {
                throw inconsistent();
            }

            inputs.add(new MatrixSeatInput(
                    cell.getSeat().getId(),
                    cell.getRowIndex(),
                    cell.getColumnIndex(),
                    cell.getNumber(),
                    cell.getSeatType()));
        }

        try {
            matrixValidator.validate(new RoomLayoutMatrixInput(
                    layout.getRowCount(),
                    layout.getColumnCount(),
                    inputs));
        } catch (ValidationException exception) {
            throw inconsistent();
        }
    }

    private ConflictException inconsistent() {
        return new ConflictException(
                InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }
}
