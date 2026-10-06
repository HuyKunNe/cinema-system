package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateRoomLayoutRequest;
import com.cinema.inventory.dto.response.RoomLayoutResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomLayoutMapper;
import com.cinema.inventory.repository.RoomLayoutRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.service.RoomLayoutService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RoomLayoutServiceImpl implements RoomLayoutService {

    private final RoomRepository roomRepository;
    private final RoomLayoutRepository roomLayoutRepository;
    private final RoomLayoutMapper roomLayoutMapper;

    public RoomLayoutServiceImpl(
            RoomRepository roomRepository,
            RoomLayoutRepository roomLayoutRepository,
            RoomLayoutMapper roomLayoutMapper) {

        this.roomRepository = roomRepository;
        this.roomLayoutRepository = roomLayoutRepository;
        this.roomLayoutMapper = roomLayoutMapper;
    }

    @Override
    @Transactional
    public RoomLayoutResponse createDraft(CreateRoomLayoutRequest request) {

        Room room =
                roomRepository
                        .findByIdForUpdate(request.roomId())
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        long maximumVersion = roomLayoutRepository.findMaximumLayoutVersionByRoomId(room.getId());

        if (maximumVersion == Long.MAX_VALUE) {
            throw new ConflictException(InventoryErrorCode.ROOM_LAYOUT_VERSION_EXHAUSTED);
        }

        RoomLayout layout =
                new RoomLayout(
                        room, maximumVersion + 1, request.canvasWidth(), request.canvasHeight());

        RoomLayout savedLayout = roomLayoutRepository.saveAndFlush(layout);

        return roomLayoutMapper.toResponse(savedLayout);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomLayoutResponse getById(UUID layoutId) {

        RoomLayout layout =
                roomLayoutRepository
                        .findById(layoutId)
                        .orElseThrow(
                                () ->
                                        new NotFoundException(
                                                InventoryErrorCode.ROOM_LAYOUT_NOT_FOUND));

        return roomLayoutMapper.toResponse(layout);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomLayoutResponse> getByRoomId(UUID roomId) {

        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND);
        }

        return roomLayoutMapper.toResponses(
                roomLayoutRepository.findAllByRoom_IdOrderByLayoutVersionDesc(roomId));
    }
}
