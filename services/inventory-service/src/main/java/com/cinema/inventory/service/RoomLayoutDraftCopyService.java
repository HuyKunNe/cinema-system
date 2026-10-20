package com.cinema.inventory.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CopyPublishedRoomLayoutRequest;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomRepository;

@Service
public class RoomLayoutDraftCopyService {

    private final RoomLayoutRepository layoutRepository;
    private final RoomRepository roomRepository;
    private final RoomLayoutMatrixCellRepository cellRepository;
    private final RoomLayoutMatrixValidator matrixValidator;
    private final RoomLayoutMatrixService matrixService;

    public RoomLayoutDraftCopyService(
            RoomLayoutRepository layoutRepository,
            RoomRepository roomRepository,
            RoomLayoutMatrixCellRepository cellRepository,
            RoomLayoutMatrixValidator matrixValidator,
            RoomLayoutMatrixService matrixService) {

        this.layoutRepository = layoutRepository;
        this.roomRepository = roomRepository;
        this.cellRepository = cellRepository;
        this.matrixValidator = matrixValidator;
        this.matrixService = matrixService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomLayoutContentResponse copy(
            UUID sourceLayoutId,
            CopyPublishedRoomLayoutRequest request) {

        RoomLayout source = layoutRepository
                .findByIdForRead(sourceLayoutId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        if (!request.expectedVersion().equals(source.getVersion())) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_VERSION_CONFLICT);
        }

        if (source.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
        }

        if (!source.isMatrixLayout()) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_MATRIX_CONVERSION_REQUIRED);
        }

        UUID roomId = source.getRoom().getId();

        // Serialize version allocation with the existing createDraft flow.
        Room room = roomRepository
                .findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_NOT_FOUND));

        RoomLayoutMatrixInput matrix = readSourceMatrix(
                source,
                roomId);

        long maximumVersion = layoutRepository
                .findMaximumLayoutVersionByRoomId(roomId);

        if (maximumVersion == Long.MAX_VALUE) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_VERSION_EXHAUSTED);
        }

        RoomLayout draft = new RoomLayout(
                room,
                maximumVersion + 1,
                BigDecimal.valueOf(matrix.columnCount()),
                BigDecimal.valueOf(matrix.rowCount() + 1L));

        draft.defineMatrix(
                matrix.rowCount(),
                matrix.columnCount());

        RoomLayout savedDraft = layoutRepository.saveAndFlush(draft);

        // Reuse the existing draft-writing path.
        // This creates new cell records while retaining inventory seat IDs.
        // It does not invoke publication or create inventory seats.
        return matrixService.replace(savedDraft, matrix);
    }

    private RoomLayoutMatrixInput readSourceMatrix(
            RoomLayout source,
            UUID roomId) {

        List<RoomLayoutMatrixCell> cells = cellRepository
                .findAllByLayout_IdOrderByRowIndexAscColumnIndexAsc(
                        source.getId());

        if (cells.isEmpty()) {
            throw inconsistentSource();
        }

        List<MatrixSeatInput> seats = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            if (cell.getSeat() == null
                    || cell.getSeat().getId() == null
                    || cell.getSeat().getRoom() == null
                    || !roomId.equals(
                            cell.getSeat().getRoom().getId())
                    || cell.getSeatType() == null) {
                throw inconsistentSource();
            }

            seats.add(new MatrixSeatInput(
                    cell.getSeat().getId(),
                    cell.getRowIndex(),
                    cell.getColumnIndex(),
                    cell.getNumber(),
                    cell.getSeatType()));
        }

        RoomLayoutMatrixInput matrix = new RoomLayoutMatrixInput(
                source.getRowCount(),
                source.getColumnCount(),
                seats);

        try {
            matrixValidator.validate(matrix);
        } catch (ValidationException exception) {
            throw inconsistentSource();
        }

        return matrix;
    }

    private ConflictException inconsistentSource() {
        return new ConflictException(
                InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }
}
