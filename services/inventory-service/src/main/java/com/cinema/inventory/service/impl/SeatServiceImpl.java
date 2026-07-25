package com.cinema.inventory.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
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

@Service
public class SeatServiceImpl implements SeatService {

  private final SeatRepository seatRepository;
  private final RoomRepository roomRepository;
  private final SeatMapper seatMapper;

  public SeatServiceImpl(
      SeatRepository seatRepository,
      RoomRepository roomRepository,
      SeatMapper seatMapper) {
    this.seatRepository = seatRepository;
    this.roomRepository = roomRepository;
    this.seatMapper = seatMapper;
  }

  @Override
  @Transactional
  public SeatResponse create(
      UUID roomId,
      CreateSeatRequest request) {

    Room room = findRoom(roomId);

    if (!room.isActive()) {
      throw new ConflictException(
          InventoryErrorCode.ROOM_INACTIVE);
    }

    String normalizedSeatNumber = normalize(request.seatNumber());

    if (seatRepository
        .existsByRoom_IdAndSeatNumberIgnoreCase(
            roomId,
            normalizedSeatNumber)) {
      throw new ConflictException(
          InventoryErrorCode.SEAT_NUMBER_ALREADY_EXISTS);
    }

    Seat seat = new Seat(
        room,
        normalizedSeatNumber,
        normalize(request.rowLabel()),
        request.seatType());

    Seat savedSeat = seatRepository.save(seat);

    return seatMapper.toResponse(savedSeat);
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

    List<Seat> seats = seatRepository
        .findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(
            roomId);

    return seatMapper.toResponses(seats);
  }

  @Override
  @Transactional
  public SeatResponse update(
      UUID seatId,
      UpdateSeatRequest request) {

    Seat seat = findSeat(seatId);
    String normalizedSeatNumber = normalize(request.seatNumber());

    UUID roomId = seat.getRoom().getId();

    if (seatRepository
        .existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
            roomId,
            normalizedSeatNumber,
            seatId)) {
      throw new ConflictException(InventoryErrorCode.SEAT_NUMBER_ALREADY_EXISTS);
    }

    seat.setSeatNumber(normalizedSeatNumber);
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
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
  }

  private Seat findSeat(UUID seatId) {
    return seatRepository.findById(seatId)
        .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SEAT_NOT_FOUND));
  }

  private String normalize(String value) {
    return value.trim();
  }
}