package com.cinema.inventory.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.service.RoomService;

@Service
public class RoomServiceImpl implements RoomService {

  private final RoomRepository roomRepository;
  private final CinemaRepository cinemaRepository;
  private final RoomMapper roomMapper;

  public RoomServiceImpl(
      RoomRepository roomRepository,
      CinemaRepository cinemaRepository,
      RoomMapper roomMapper) {
    this.roomRepository = roomRepository;
    this.cinemaRepository = cinemaRepository;
    this.roomMapper = roomMapper;
  }

  @Override
  @Transactional
  public RoomResponse create(UUID cinemaId, CreateRoomRequest request) {

    Cinema cinema = findCinema(cinemaId);

    if (!cinema.isActive()) {
      throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
    }

    String normalizedName = normalize(request.name());

    if (roomRepository.existsByCinema_IdAndNameIgnoreCase(
        cinemaId,
        normalizedName)) {
      throw new ConflictException(InventoryErrorCode.ROOM_NAME_ALREADY_EXISTS);
    }

    Room room = new Room(
        cinema,
        normalizedName,
        request.roomType());

    Room savedRoom = roomRepository.save(room);

    return roomMapper.toResponse(savedRoom);
  }

  @Override
  @Transactional(readOnly = true)
  public RoomResponse getById(UUID roomId) {
    return roomMapper.toResponse(findRoom(roomId));
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoomResponse> getActiveRooms(UUID cinemaId) {
    findCinema(cinemaId);

    List<Room> rooms = roomRepository
        .findAllByCinema_IdAndActiveTrueOrderByNameAsc(cinemaId);

    return roomMapper.toResponses(rooms);
  }

  @Override
  @Transactional
  public RoomResponse update(UUID roomId, UpdateRoomRequest request) {

    Room room = findRoom(roomId);
    String normalizedName = normalize(request.name());

    if (roomRepository.existsByCinema_IdAndNameIgnoreCaseAndIdNot(
        room.getCinema().getId(),
        normalizedName,
        roomId)) {
      throw new ConflictException(InventoryErrorCode.ROOM_NAME_ALREADY_EXISTS);
    }

    room.setName(normalizedName);
    room.setRoomType(request.roomType());

    if (request.active()) {
      room.activate();
    } else {
      room.deactivate();
    }

    return roomMapper.toResponse(room);
  }

  private Cinema findCinema(UUID cinemaId) {
    return cinemaRepository.findById(cinemaId)
        .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));
  }

  private Room findRoom(UUID roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
  }

  private String normalize(String value) {
    return value.trim();
  }
}