package com.cinema.inventory.service;

import com.cinema.common.api.screening.ReplaceScreeningCapabilitiesRequest;
import com.cinema.common.api.screening.ScreeningFormat;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.response.RoomScreeningCapabilitiesResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.repository.RoomRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RoomScreeningCapabilitiesService {

    private final RoomRepository roomRepository;

    public RoomScreeningCapabilitiesService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional
    public RoomScreeningCapabilitiesResponse get(UUID roomId) {
        Room room =
                roomRepository
                        .findByIdForRead(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        return response(room);
    }

    @Transactional
    public RoomScreeningCapabilitiesResponse replace(
            UUID roomId, ReplaceScreeningCapabilitiesRequest request) {

        Room room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        if (!room.getVersion().equals(request.expectedVersion())) {
            throw new ConflictException(InventoryErrorCode.ROOM_SCREENING_VERSION_CONFLICT);
        }

        room.replaceScreeningFormats(request.formats());
        roomRepository.flush();

        return response(room);
    }

    private RoomScreeningCapabilitiesResponse response(Room room) {
        return new RoomScreeningCapabilitiesResponse(
                room.getId(),
                room.getVersion(),
                ScreeningFormat.options(room.getScreeningFormats()),
                ScreeningFormat.allOptions());
    }
}
