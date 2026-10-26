package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.config.BookableShowtimeProperties;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScheduling;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowtimeCreationService;
import com.cinema.inventory.service.ShowtimeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final Set<ShowtimeStatus> NON_BLOCKING_STATUSES =
            EnumSet.of(ShowtimeStatus.CANCELLED);

    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeMapper showtimeMapper;
    private final Clock clock;
    private final BookableShowtimeProperties bookableShowtimeProperties;
    private final ShowtimeCreationService showtimeCreationService;

    public ShowtimeServiceImpl(
            ShowtimeRepository showtimeRepository,
            RoomRepository roomRepository,
            ShowtimeMapper showtimeMapper,
            Clock clock,
            BookableShowtimeProperties bookableShowtimeProperties,
            ShowtimeCreationService showtimeCreationService) {

        this.showtimeRepository = showtimeRepository;
        this.roomRepository = roomRepository;
        this.showtimeMapper = showtimeMapper;
        this.clock = clock;
        this.bookableShowtimeProperties = bookableShowtimeProperties;
        this.showtimeCreationService = showtimeCreationService;
    }

    @Override
    public ShowtimeResponse create(CreateShowtimeRequest request) {
        return showtimeCreationService.create(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getById(UUID showtimeId) {
        return showtimeMapper.toResponse(findShowtime(showtimeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByRoomId(UUID roomId) {
        findRoom(roomId);

        List<Showtime> showtimes = showtimeRepository.findAllByRoom_IdOrderByStartsAtAsc(roomId);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByMovieId(UUID movieId) {
        List<Showtime> showtimes = showtimeRepository.findAllByMovieIdOrderByStartsAtAsc(movieId);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByTimeRange(OffsetDateTime from, OffsetDateTime to) {

        validateTimeRange(from, to);

        List<Showtime> showtimes =
                showtimeRepository.findAllByStartsAtBetweenOrderByStartsAtAsc(from, to);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getBookableShowtimes(
            UUID cinemaId, UUID movieId, OffsetDateTime from, OffsetDateTime to) {

        validateTimeRange(from, to);

        Duration maximumRange = bookableShowtimeProperties.getMaximumRange();

        Duration requestedRange = Duration.between(from.toInstant(), to.toInstant());

        if (maximumRange != null && requestedRange.compareTo(maximumRange) > 0) {

            throw new ValidationException(InventoryErrorCode.SHOWTIME_QUERY_RANGE_TOO_LARGE);
        }

        OffsetDateTime now = OffsetDateTime.now(clock);

        List<Showtime> showtimes =
                showtimeRepository.findBookableShowtimes(
                        cinemaId, movieId, from, to, now, ShowtimeStatus.OPEN_FOR_BOOKING);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse update(UUID showtimeId, UpdateShowtimeRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || (request.status() != null && request.status() != ShowtimeStatus.SCHEDULED)) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_UPDATE);
        }

        validateTimeRange(request.startsAt(), request.endsAt());
        ShowtimeScheduling.validateBreak(request.breakMinutes());

        UUID roomId =
                showtimeRepository
                        .findRoomIdForScheduleChange(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        Room room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        Showtime showtime = findShowtimeForUpdate(showtimeId);

        if (!request.expectedVersion().equals(showtime.getVersion())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_VERSION_CONFLICT);
        }

        if (showtime.getStatus() != ShowtimeStatus.SCHEDULED) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }

        validateRoomAvailable(room);

        if (!request.startsAt().isAfter(OffsetDateTime.now(clock))) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_START_NOT_FUTURE);
        }

        showtime.changeSchedule(request.startsAt(), request.endsAt(), request.breakMinutes());

        boolean conflict =
                showtimeRepository.existsSchedulingConflict(
                        roomId,
                        showtimeId,
                        showtime.getStartsAt(),
                        showtime.getScheduling().getReadyAt(),
                        NON_BLOCKING_STATUSES);

        if (conflict) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }

        return flushAndMap(showtime);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ShowtimeResponse openForBooking(UUID showtimeId) {

        UUID roomId =
                showtimeRepository
                        .findRoomIdForScheduleChange(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        Room room =
                roomRepository
                        .findByIdForUpdate(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        Showtime showtime = findShowtimeForUpdate(showtimeId);

        validateRoomAvailable(room);

        if (!showtime.getStartsAt().isAfter(OffsetDateTime.now(clock))) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
        }

        showtime.openForBooking();

        return flushAndMap(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse close(UUID showtimeId) {
        Showtime showtime = findShowtimeForUpdate(showtimeId);
        showtime.close();
        return flushAndMap(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse cancel(UUID showtimeId) {
        Showtime showtime = findShowtimeForUpdate(showtimeId);
        showtime.cancel();
        return flushAndMap(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse complete(UUID showtimeId) {
        Showtime showtime = findShowtimeForUpdate(showtimeId);
        showtime.complete();
        return flushAndMap(showtime);
    }

    private Room findRoom(UUID roomId) {
        return roomRepository
                .findById(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
    }

    private Showtime findShowtime(UUID showtimeId) {
        return showtimeRepository
                .findById(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }

    private Showtime findShowtimeForUpdate(UUID showtimeId) {
        return showtimeRepository
                .findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }

    private ShowtimeResponse flushAndMap(Showtime showtime) {
        showtimeRepository.flush();
        return showtimeMapper.toResponse(showtime);
    }

    private void validateRoomAvailable(Room room) {
        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        if (!room.getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }
    }

    private void validateTimeRange(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        if (startsAt == null || endsAt == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PERIOD);
        }
    }
}
