package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.enums.ContentFormat;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.RoomMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.service.RoomService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final RoomMapper roomMapper;
    private final Clock clock;

    public RoomServiceImpl(
            RoomRepository roomRepository,
            CinemaRepository cinemaRepository,
            RoomMapper roomMapper,
            Clock clock) {

        this.roomRepository = roomRepository;
        this.cinemaRepository = cinemaRepository;
        this.roomMapper = roomMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public RoomResponse create(UUID cinemaId, CreateRoomRequest request) {

        Set<ContentFormat> contentFormats = normalizeContentFormats(request.contentFormats());

        Cinema cinema = findCinema(cinemaId);

        if (!cinema.isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }

        String normalizedName = normalize(request.name());

        if (roomRepository.existsByCinema_IdAndNameIgnoreCase(cinemaId, normalizedName)) {

            throw new ConflictException(InventoryErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        Room room = new Room(cinema, normalizedName, request.roomType());

        room.replaceSupportedContentFormats(contentFormats);

        if (Boolean.FALSE.equals(request.active())) {
            room.deactivate();
        }

        Room savedRoom = roomRepository.saveAndFlush(room);

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

        List<Room> rooms = roomRepository.findAllByCinema_IdAndActiveTrueOrderByNameAsc(cinemaId);

        return roomMapper.toResponses(rooms);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getManagedRooms(UUID cinemaId, Boolean active) {

        findCinema(cinemaId);

        List<Room> rooms =
                active == null
                        ? roomRepository.findAllByCinema_IdOrderByNameAscIdAsc(cinemaId)
                        : roomRepository.findAllByCinema_IdAndActiveOrderByNameAscIdAsc(
                                cinemaId, active);

        return roomMapper.toResponses(rooms);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RoomResponse update(UUID roomId, UpdateRoomRequest request) {

        Room room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        String normalizedName = normalize(request.name());

        if (roomRepository.existsByCinema_IdAndNameIgnoreCaseAndIdNot(
                room.getCinema().getId(), normalizedName, roomId)) {

            throw new ConflictException(InventoryErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        boolean changingRoomType = room.getRoomType() != request.roomType();

        if (changingRoomType
                && roomRepository.existsShowtimesWithStatusNot(roomId, ShowtimeStatus.CANCELLED)) {

            throw new ConflictException(InventoryErrorCode.ROOM_TYPE_CHANGE_BLOCKED);
        }

        if (!request.active()
                && roomRepository.existsShowtimeWithStatusStartingAtOrAfter(
                        roomId, ShowtimeStatus.OPEN_FOR_BOOKING, startOfTodayAtCinema(room))) {

            throw new ConflictException(InventoryErrorCode.ROOM_DEACTIVATION_BLOCKED);
        }

        room.setName(normalizedName);
        room.setRoomType(request.roomType());

        if (request.active()) {
            room.activate();
        } else {
            room.deactivate();
        }

        roomRepository.flush();

        return roomMapper.toResponse(room);
    }

    private Cinema findCinema(UUID cinemaId) {
        return cinemaRepository
                .findById(cinemaId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));
    }

    private Room findRoom(UUID roomId) {
        return roomRepository
                .findById(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
    }

    private Set<ContentFormat> normalizeContentFormats(List<ContentFormat> requestedFormats) {

        Set<ContentFormat> formats = EnumSet.noneOf(ContentFormat.class);

        if (requestedFormats == null) {
            return formats;
        }

        if (requestedFormats.size() > ContentFormat.values().length) {
            throw new ValidationException(InventoryErrorCode.ROOM_CONTENT_FORMATS_INVALID);
        }

        for (ContentFormat format : requestedFormats) {
            if (format == null || !formats.add(format)) {
                throw new ValidationException(InventoryErrorCode.ROOM_CONTENT_FORMATS_INVALID);
            }
        }

        return formats;
    }

    private OffsetDateTime startOfTodayAtCinema(Room room) {
        String configuredZone = room.getCinema().getTimeZone();

        String zoneName = configuredZone == null ? "Asia/Ho_Chi_Minh" : configuredZone;

        if (!ZoneId.getAvailableZoneIds().contains(zoneName)) {
            throw new ValidationException(InventoryErrorCode.CINEMA_TIME_ZONE_INVALID);
        }

        ZoneId cinemaZone = ZoneId.of(zoneName);

        return clock.instant()
                .atZone(cinemaZone)
                .toLocalDate()
                .atStartOfDay(cinemaZone)
                .toOffsetDateTime();
    }

    private String normalize(String value) {
        return value.trim();
    }
}
