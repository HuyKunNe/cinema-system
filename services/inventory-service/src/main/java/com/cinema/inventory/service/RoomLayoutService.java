package com.cinema.inventory.service;

import com.cinema.inventory.dto.request.CreateRoomLayoutRequest;
import com.cinema.inventory.dto.response.RoomLayoutResponse;

import java.util.List;
import java.util.UUID;

public interface RoomLayoutService {

    RoomLayoutResponse createDraft(CreateRoomLayoutRequest request);

    RoomLayoutResponse getById(UUID layoutId);

    List<RoomLayoutResponse> getByRoomId(UUID roomId);
}
