package com.cinema.inventory.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.RoomLayoutSeat;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomLayoutStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomLayoutSeatRepository;
import com.cinema.inventory.repository.SeatRepository;

@Service
public class ShowtimeLayoutService {

    private final RoomLayoutRepository layoutRepository;
    private final RoomLayoutSeatRepository layoutSeatRepository;
    private final SeatRepository seatRepository;

    public ShowtimeLayoutService(
            RoomLayoutRepository layoutRepository,
            RoomLayoutSeatRepository layoutSeatRepository,
            SeatRepository seatRepository) {

        this.layoutRepository = layoutRepository;
        this.layoutSeatRepository = layoutSeatRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public RoomLayout findPublishedLayout(UUID layoutId, UUID roomId) {
        RoomLayout layout = layoutRepository
                .findByIdForRead(layoutId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        if (!layout.getRoom().getId().equals(roomId)) {
            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_LAYOUT_ROOM_MISMATCH);
        }

        if (layout.getStatus() != RoomLayoutStatus.PUBLISHED) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_NOT_PUBLISHED);
        }

        return layout;
    }

    @Transactional
    public List<Seat> getSeatsForGeneration(Showtime showtime) {
        if (showtime.getRoomLayout() == null) {
            throw new ValidationException(
                    InventoryErrorCode.ROOM_LAYOUT_DATA_INVALID);
        }

        UUID roomId = showtime.getRoom().getId();

        RoomLayout layout = findPublishedLayout(
                showtime.getRoomLayout().getId(),
                roomId);

        List<RoomLayoutSeat> positions = layoutSeatRepository
                .findPositionsForGenerationByLayoutId(layout.getId());

        if (positions.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_LAYOUT_INCOMPLETE);
        }

        List<UUID> seatIds = positions.stream()
                .map(position -> position.getSeat().getId())
                .toList();

        Map<UUID, Seat> seatsById = new HashMap<>();

        for (Seat seat : seatRepository.findAllForLayoutGeneration(roomId, seatIds)) {
            seatsById.put(seat.getId(), seat);
        }

        List<Seat> result = new ArrayList<>();

        for (RoomLayoutSeat position : positions) {
            Seat seat = seatsById.get(position.getSeat().getId());

            if (seat == null
                    || !Objects.equals(
                            seat.getSeatNumber(),
                            position.getSeatNumberSnapshot())
                    || !Objects.equals(
                            seat.getRowLabel(),
                            position.getRowLabelSnapshot())
                    || seat.getSeatType() != position.getSeatTypeSnapshot()) {

                throw new ConflictException(
                        InventoryErrorCode.ROOM_LAYOUT_SEAT_SNAPSHOT_MISMATCH);
            }

            result.add(seat);
        }

        return List.copyOf(result);
    }
}
