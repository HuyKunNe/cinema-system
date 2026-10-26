package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput;
import com.cinema.inventory.dto.request.RoomLayoutMatrixInput.MatrixSeatInput;
import com.cinema.inventory.dto.response.CinemaRoomCapacitiesResponse;
import com.cinema.inventory.dto.response.CinemaRoomCapacitiesResponse.CapacitySource;
import com.cinema.inventory.dto.response.CinemaRoomCapacitiesResponse.RoomCapacity;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutMatrixCell;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomLayoutMatrixCellRepository;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.RoomRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CinemaRoomCapacitiesService {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutMatrixCellRepository cellRepository;
    private final RoomLayoutSeatRepository positionRepository;
    private final RoomLayoutMatrixValidator matrixValidator;

    public CinemaRoomCapacitiesService(
            CinemaRepository cinemaRepository,
            RoomRepository roomRepository,
            RoomLayoutRepository layoutRepository,
            RoomLayoutMatrixCellRepository cellRepository,
            RoomLayoutSeatRepository positionRepository,
            RoomLayoutMatrixValidator matrixValidator) {

        this.cinemaRepository = cinemaRepository;
        this.roomRepository = roomRepository;
        this.layoutRepository = layoutRepository;
        this.cellRepository = cellRepository;
        this.positionRepository = positionRepository;
        this.matrixValidator = matrixValidator;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CinemaRoomCapacitiesResponse get(UUID cinemaId) {

        if (!cinemaRepository.existsById(cinemaId)) {
            throw new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND);
        }

        List<Room> rooms = roomRepository.findAllByCinema_IdOrderByNameAscIdAsc(cinemaId);

        if (rooms.isEmpty()) {
            return new CinemaRoomCapacitiesResponse(cinemaId, 0, 0, 0, 0, true, 0, 0L, List.of());
        }

        List<UUID> roomIds = rooms.stream().map(Room::getId).toList();

        List<RoomLayout> layouts =
                layoutRepository.findLatestPublishedForRooms(roomIds, RoomLayoutStatus.PUBLISHED);

        Map<UUID, RoomLayout> layoutsByRoom = new HashMap<>();
        List<UUID> matrixIds = new ArrayList<>();
        List<UUID> geometryIds = new ArrayList<>();

        for (RoomLayout layout : layouts) {
            if ((layout.getRowCount() == null) != (layout.getColumnCount() == null)) {
                throw inconsistent();
            }

            UUID roomId = layout.getRoom().getId();

            if (layoutsByRoom.putIfAbsent(roomId, layout) != null) {
                throw inconsistent();
            }

            if (layout.isMatrixLayout()) {
                matrixIds.add(layout.getId());
            } else {
                geometryIds.add(layout.getId());
            }
        }

        List<RoomLayoutMatrixCell> cells =
                matrixIds.isEmpty()
                        ? List.of()
                        : cellRepository.findAllForCapacityByLayoutIds(matrixIds);

        List<RoomLayoutSeat> positions =
                geometryIds.isEmpty()
                        ? List.of()
                        : positionRepository.findAllForCapacityByLayoutIds(geometryIds);

        Map<UUID, List<RoomLayoutMatrixCell>> cellsByLayout =
                cells.stream().collect(Collectors.groupingBy(cell -> cell.getLayout().getId()));

        Map<UUID, List<RoomLayoutSeat>> positionsByLayout =
                positions.stream()
                        .collect(Collectors.groupingBy(position -> position.getLayout().getId()));

        List<RoomCapacity> summaries = new ArrayList<>();
        long knownCapacity = 0;
        long unconfiguredRoomCount = 0;

        for (Room room : rooms) {
            RoomLayout layout = layoutsByRoom.get(room.getId());

            if (layout == null) {
                unconfiguredRoomCount++;

                summaries.add(
                        new RoomCapacity(
                                room.getId(),
                                room.getName(),
                                room.isActive(),
                                null,
                                null,
                                CapacitySource.NOT_PUBLISHED,
                                null,
                                null,
                                null,
                                null));

                continue;
            }

            RoomCapacity summary =
                    layout.isMatrixLayout()
                            ? fromMatrix(
                                    room,
                                    layout,
                                    cellsByLayout.getOrDefault(layout.getId(), List.of()))
                            : fromGeometry(
                                    room,
                                    layout,
                                    positionsByLayout.getOrDefault(layout.getId(), List.of()));

            knownCapacity += summary.capacity();
            summaries.add(summary);
        }

        long activeRoomCount = rooms.stream().filter(Room::isActive).count();

        boolean complete = unconfiguredRoomCount == 0;

        return new CinemaRoomCapacitiesResponse(
                cinemaId,
                rooms.size(),
                activeRoomCount,
                rooms.size() - activeRoomCount,
                unconfiguredRoomCount,
                complete,
                knownCapacity,
                complete ? knownCapacity : null,
                List.copyOf(summaries));
    }

    private RoomCapacity fromMatrix(
            Room room, RoomLayout layout, List<RoomLayoutMatrixCell> cells) {

        List<MatrixSeatInput> inputs = new ArrayList<>();
        List<CapacitySeat> seats = new ArrayList<>();

        for (RoomLayoutMatrixCell cell : cells) {
            CapacitySeat seat = snapshot(room, cell.getSeat(), cell.getSeatType());

            seats.add(seat);

            inputs.add(
                    new MatrixSeatInput(
                            seat.id(),
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
            throw inconsistent();
        }

        return count(room, layout, CapacitySource.PUBLISHED_MATRIX, seats);
    }

    private RoomCapacity fromGeometry(
            Room room, RoomLayout layout, List<RoomLayoutSeat> positions) {

        List<CapacitySeat> seats =
                positions.stream()
                        .map(
                                position ->
                                        snapshot(
                                                room,
                                                position.getSeat(),
                                                position.getSeatTypeSnapshot()))
                        .toList();

        return count(room, layout, CapacitySource.PUBLISHED_GEOMETRY, seats);
    }

    private CapacitySeat snapshot(Room room, Seat seat, SeatType snapshotType) {

        if (seat == null
                || seat.getId() == null
                || seat.getRoom() == null
                || !room.getId().equals(seat.getRoom().getId())
                || snapshotType == null) {

            throw inconsistent();
        }

        return new CapacitySeat(seat.getId(), snapshotType, seat.isActive());
    }

    private RoomCapacity count(
            Room room, RoomLayout layout, CapacitySource source, List<CapacitySeat> seats) {

        if (seats.isEmpty()) {
            throw inconsistent();
        }

        Set<UUID> ids = new HashSet<>();
        long capacity = 0;
        long activeUnits = 0;
        long activeCapacity = 0;

        for (CapacitySeat seat : seats) {
            if (!ids.add(seat.id())) {
                throw inconsistent();
            }

            int places = seat.type().getCapacity();
            capacity += places;

            if (seat.active()) {
                activeUnits++;
                activeCapacity += places;
            }
        }

        return new RoomCapacity(
                room.getId(),
                room.getName(),
                room.isActive(),
                layout.getId(),
                layout.getLayoutVersion(),
                source,
                (long) seats.size(),
                capacity,
                activeUnits,
                activeCapacity);
    }

    private static ConflictException inconsistent() {
        return new ConflictException(InventoryErrorCode.SEAT_MAP_DATA_INCONSISTENT);
    }

    private record CapacitySeat(UUID id, SeatType type, boolean active) {}
}
