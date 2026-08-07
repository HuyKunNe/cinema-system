package com.cinema.inventory.service.impl;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowSeatGenerationService;
import com.cinema.inventory.service.ShowtimeService;

@Service
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final Set<ShowtimeStatus> NON_BLOCKING_STATUSES = EnumSet.of(ShowtimeStatus.CANCELLED);

    private final ShowtimeRepository showtimeRepository;
    private final RoomRepository roomRepository;
    private final ShowtimeMapper showtimeMapper;
    private final ShowSeatGenerationService showSeatGenerationService;

    public ShowtimeServiceImpl(
            ShowtimeRepository showtimeRepository,
            RoomRepository roomRepository,
            ShowtimeMapper showtimeMapper,
            ShowSeatGenerationService showSeatGenerationService) {

        this.showtimeRepository = showtimeRepository;
        this.roomRepository = roomRepository;
        this.showtimeMapper = showtimeMapper;
        this.showSeatGenerationService = showSeatGenerationService;
    }

    @Override
    @Transactional
    public ShowtimeResponse create(
            CreateShowtimeRequest request) {

        Room room = findRoom(request.roomId());

        validateRoomAvailable(room);

        validateNoOverlap(
                room.getId(),
                request.startsAt(),
                request.endsAt());

        Showtime showtime = new Showtime(
                request.movieId(),
                room,
                request.startsAt(),
                request.endsAt());

        Showtime savedShowtime = showtimeRepository.save(showtime);

        showSeatGenerationService.generate(
                savedShowtime,
                request.basePrice());

        return showtimeMapper.toResponse(savedShowtime);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse getById(UUID showtimeId) {
        return showtimeMapper.toResponse(
                findShowtime(showtimeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByRoomId(UUID roomId) {
        findRoom(roomId);

        List<Showtime> showtimes = showtimeRepository
                .findAllByRoom_IdOrderByStartsAtAsc(roomId);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByMovieId(UUID movieId) {
        List<Showtime> showtimes = showtimeRepository
                .findAllByMovieIdOrderByStartsAtAsc(movieId);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowtimeResponse> getByTimeRange(
            OffsetDateTime from,
            OffsetDateTime to) {

        validateTimeRange(from, to);

        List<Showtime> showtimes = showtimeRepository
                .findAllByStartsAtBetweenOrderByStartsAtAsc(
                        from,
                        to);

        return showtimeMapper.toResponses(showtimes);
    }

    @Override
    @Transactional
    public ShowtimeResponse update(
            UUID showtimeId,
            UpdateShowtimeRequest request) {

        Showtime showtime = findShowtime(showtimeId);

        validateNoOverlapExcludingId(
                showtime.getRoom().getId(),
                showtimeId,
                request.startsAt(),
                request.endsAt());

        showtime.changeSchedule(
                request.startsAt(),
                request.endsAt());

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse openForBooking(UUID showtimeId) {
        Showtime showtime = findShowtime(showtimeId);

        validateRoomAvailable(showtime.getRoom());
        showtime.openForBooking();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse close(UUID showtimeId) {
        Showtime showtime = findShowtime(showtimeId);
        showtime.close();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse cancel(UUID showtimeId) {
        Showtime showtime = findShowtime(showtimeId);
        showtime.cancel();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse complete(UUID showtimeId) {
        Showtime showtime = findShowtime(showtimeId);
        showtime.complete();

        return showtimeMapper.toResponse(showtime);
    }

    private Room findRoom(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));
    }

    private Showtime findShowtime(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }

    private void validateRoomAvailable(Room room) {
        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        if (!room.getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }
    }

    private void validateNoOverlap(
            UUID roomId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        boolean overlapping = showtimeRepository.existsOverlappingShowtime(
                roomId,
                startsAt,
                endsAt,
                NON_BLOCKING_STATUSES);

        if (overlapping) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }
    }

    private void validateNoOverlapExcludingId(
            UUID roomId,
            UUID showtimeId,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        boolean overlapping = showtimeRepository
                .existsOverlappingShowtimeExcludingId(
                        roomId,
                        showtimeId,
                        startsAt,
                        endsAt,
                        NON_BLOCKING_STATUSES);

        if (overlapping) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }
    }

    private void validateTimeRange(
            OffsetDateTime startsAt,
            OffsetDateTime endsAt) {

        if (startsAt == null || endsAt == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PERIOD);
        }
    }
}
