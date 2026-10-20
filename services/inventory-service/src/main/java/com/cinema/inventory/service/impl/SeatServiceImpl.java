package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateSeatRangeRequest;
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.SeatMapper;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.service.SeatService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final SeatMapper seatMapper;

    public SeatServiceImpl(
            SeatRepository seatRepository, RoomRepository roomRepository, SeatMapper seatMapper) {

        this.seatRepository = seatRepository;
        this.roomRepository = roomRepository;
        this.seatMapper = seatMapper;
    }

    @Override
    @Transactional
    public SeatResponse create(UUID roomId, CreateSeatRequest request) {

        Room room = findRoom(roomId);

        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        String seatNumber = normalize(request.seatNumber());

        if (seatRepository.existsByRoom_IdAndSeatNumberIgnoreCase(roomId, seatNumber)) {
            throw new ConflictException(InventoryErrorCode.SEAT_NUMBER_ALREADY_EXISTS);
        }

        Seat seat = new Seat(room, seatNumber, normalize(request.rowLabel()), request.seatType());

        return seatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional
    public List<SeatResponse> createRange(UUID roomId, CreateSeatRangeRequest request) {

        Room room = findRoom(roomId);

        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        String rowLabel = normalize(request.rowLabel()).toUpperCase(Locale.ROOT);

        Set<String> existingSeatNumbers = new HashSet<>();

        seatRepository.findAllByRoom_IdOrderBySeatNumberAsc(roomId).stream()
                .map(Seat::getSeatNumber)
                .map(number -> number.toLowerCase(Locale.ROOT))
                .forEach(existingSeatNumbers::add);

        List<Seat> seats = new ArrayList<>();

        for (int number = request.startNumber(); number <= request.endNumber(); number++) {

            String seatNumber = rowLabel + number;

            if (existingSeatNumbers.contains(seatNumber.toLowerCase(Locale.ROOT))) {
                throw new ConflictException(InventoryErrorCode.SEAT_NUMBER_ALREADY_EXISTS);
            }

            seats.add(new Seat(room, seatNumber, rowLabel, request.seatType()));
        }

        return seatMapper.toResponses(seatRepository.saveAll(seats));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getById(UUID seatId) {
        return seatMapper.toResponse(findSeat(seatId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getActiveSeats(UUID roomId) {
        findRoom(roomId);

        return seatMapper.toResponses(
                seatRepository.findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(roomId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getManagedSeats(UUID roomId, Boolean active) {

        findRoom(roomId);

        List<Seat> seats =
                active == null
                        ? seatRepository.findAllByRoom_IdOrderBySeatNumberAscIdAsc(roomId)
                        : seatRepository.findAllByRoom_IdAndActiveOrderBySeatNumberAscIdAsc(
                                roomId, active);

        return seatMapper.toResponses(seats);
    }

    @Override
    @Transactional
    public SeatResponse update(UUID seatId, UpdateSeatRequest request) {

        Seat seat = findSeat(seatId);
        String seatNumber = normalize(request.seatNumber());

        if (!seat.isLayoutManaged()
                && seatRepository.existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
                        seat.getRoom().getId(), seatNumber, seatId)) {
            throw new ConflictException(InventoryErrorCode.SEAT_NUMBER_ALREADY_EXISTS);
        }

        // Layout-managed seats reject metadata changes in the entity.
        // Unchanged metadata still permits an active-state update.
        seat.setSeatNumber(seatNumber);
        seat.setRowLabel(normalize(request.rowLabel()));
        seat.setSeatType(request.seatType());

        if (request.active()) {
            seat.activate();
        } else {
            seat.deactivate();
        }

        return seatMapper.toResponse(seat);
    }

    private Room findRoom(UUID roomId) {
        return roomRepository
                .findById(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
    }

    private Seat findSeat(UUID seatId) {
        return seatRepository
                .findById(seatId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SEAT_NOT_FOUND));
    }

    private String normalize(String value) {
        return value.trim();
    }
}
