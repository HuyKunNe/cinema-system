package com.cinema.inventory.service;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;

public interface RoomService {

  RoomResponse create(UUID cinemaId, CreateRoomRequest request);

  RoomResponse getById(UUID roomId);

  List<RoomResponse> getActiveRooms(UUID cinemaId);

  RoomResponse update(UUID roomId, UpdateRoomRequest request);
}