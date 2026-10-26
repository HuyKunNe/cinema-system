package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceRoomContentFormatsRequest;
import com.cinema.inventory.dto.response.RoomContentFormatsResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomContentFormatsMapper;
import com.cinema.inventory.repository.RoomRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
public class RoomContentFormatsService {

    private final RoomRepository roomRepository;
    private final RoomContentFormatsMapper mapper;

    public RoomContentFormatsService(
            RoomRepository roomRepository, RoomContentFormatsMapper mapper) {

        this.roomRepository = roomRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public RoomContentFormatsResponse get(UUID roomId) {
        Room room =
                roomRepository
                        .findById(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        return mapper.toResponse(room);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomContentFormatsResponse replace(
            UUID roomId, ReplaceRoomContentFormatsRequest request) {

        Set<ContentFormat> formats = validateAndNormalize(request);

        Room room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        if (!request.expectedVersion().equals(room.getVersion())) {
            throw new ConflictException(InventoryErrorCode.ROOM_VERSION_CONFLICT);
        }

        boolean removingExistingFormats = !formats.containsAll(room.getSupportedContentFormats());

        if (removingExistingFormats
                && roomRepository.existsShowtimesWithStatusNot(roomId, ShowtimeStatus.CANCELLED)) {

            throw new ConflictException(InventoryErrorCode.ROOM_FORMAT_REMOVAL_BLOCKED);
        }

        room.replaceSupportedContentFormats(formats);

        roomRepository.flush();

        return mapper.toResponse(room);
    }

    private Set<ContentFormat> validateAndNormalize(ReplaceRoomContentFormatsRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || request.formats() == null
                || request.formats().size() > ContentFormat.values().length) {

            throw invalid();
        }

        Set<ContentFormat> formats = EnumSet.noneOf(ContentFormat.class);

        for (ContentFormat format : request.formats()) {
            if (format == null || !formats.add(format)) {
                throw invalid();
            }
        }

        return formats;
    }

    private static ValidationException invalid() {
        return new ValidationException(InventoryErrorCode.ROOM_CONTENT_FORMATS_INVALID);
    }
}
