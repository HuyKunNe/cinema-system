package com.cinema.inventory.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieScreeningClient;
import com.cinema.inventory.config.BookableShowtimeProperties;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.CinemaPricingPolicy;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.RoomLayout;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowSeatGenerationService;
import com.cinema.inventory.service.ShowtimeLayoutService;
import com.cinema.inventory.service.ShowtimePreparationValidator;
import com.cinema.inventory.service.ShowtimeService;
import com.cinema.inventory.service.pricing.PublishedCinemaPricingPolicyResolver;

import org.springframework.stereotype.Service;
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
    private final ShowSeatGenerationService showSeatGenerationService;
    private final Clock clock;
    private final BookableShowtimeProperties bookableShowtimeProperties;
    private final ShowtimeLayoutService showtimeLayoutService;
    private final CinemaRepository cinemaRepository;
    private final MovieScreeningClient movieClient;
    private final PublishedCinemaPricingPolicyResolver policyResolver;
    private final ShowtimePreparationValidator preparationValidator;

    public ShowtimeServiceImpl(
            ShowtimeRepository showtimeRepository,
            RoomRepository roomRepository,
            ShowtimeMapper showtimeMapper,
            ShowSeatGenerationService showSeatGenerationService,
            Clock clock,
            BookableShowtimeProperties bookableShowtimeProperties,
            ShowtimeLayoutService showtimeLayoutService,
            CinemaRepository cinemaRepository,
            MovieScreeningClient movieClient,
            PublishedCinemaPricingPolicyResolver policyResolver,
            ShowtimePreparationValidator preparationValidator) {

        this.showtimeRepository = showtimeRepository;
        this.roomRepository = roomRepository;
        this.showtimeMapper = showtimeMapper;
        this.showSeatGenerationService = showSeatGenerationService;
        this.clock = clock;
        this.bookableShowtimeProperties = bookableShowtimeProperties;
        this.showtimeLayoutService = showtimeLayoutService;
        this.cinemaRepository = cinemaRepository;
        this.movieClient = movieClient;
        this.policyResolver = policyResolver;
        this.preparationValidator = preparationValidator;
    }

    @Override
    @Transactional
    public ShowtimeResponse create(CreateShowtimeRequest request) {
        // Fetch remote data before acquiring local database locks.
        var movie = movieClient.get(request.movieId());

        UUID cinemaId =
                roomRepository
                        .findCinemaIdByRoomId(request.roomId())
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        // Locks cinema and reads published policy before locking room.
        CinemaPricingPolicy policy = policyResolver.resolveForStart(cinemaId, request.startsAt());

        Room room =
                roomRepository
                        .findByIdForUpdate(request.roomId())
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        validateRoomAvailable(room);

        preparationValidator.requireScreening(
                room, movie, request.experienceType(), request.contentFormat());

        RoomLayout layout = showtimeLayoutService.resolveLatestPublished(room.getId());

        validateNoOverlap(room.getId(), request.startsAt(), request.endsAt());

        Showtime showtime =
                Showtime.withPricingPolicy(
                        request.movieId(),
                        room,
                        request.startsAt(),
                        request.endsAt(),
                        layout,
                        policy,
                        request.experienceType(),
                        request.contentFormat(),
                        movie.releaseDate(),
                        movie.version());

        var snapshots = showtimeLayoutService.getSeatSnapshotsForGeneration(showtime);

        preparationValidator.requirePolicyCoverage(
                policy,
                request.experienceType(),
                request.contentFormat(),
                movie.releaseDate(),
                request.startsAt(),
                snapshots);

        Showtime saved = showtimeRepository.save(showtime);

        showSeatGenerationService.generate(saved);
        showtimeRepository.flush();

        return showtimeMapper.toResponse(saved);
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
    @Transactional
    public ShowtimeResponse update(UUID showtimeId, UpdateShowtimeRequest request) {

        Showtime showtime = lockForMutation(showtimeId);

        if (showtime.usesPolicyPricing()) {
            if (showtime.getStatus() != ShowtimeStatus.SCHEDULED
                    || request.status() != ShowtimeStatus.SCHEDULED) {
                throw new ConflictException(InventoryErrorCode.SHOWTIME_NOT_EDITABLE);
            }

            validateRoomAvailable(showtime.getRoom());

            var movie = movieClient.get(showtime.getMovieId());

            preparationValidator.requireScreening(
                    showtime.getRoom(),
                    movie,
                    showtime.getExperienceType(),
                    showtime.getContentFormat());

            if (!movie.releaseDate().equals(showtime.getOfficialReleaseDate())) {
                throw new ConflictException(InventoryErrorCode.SHOWTIME_MOVIE_RELEASE_CHANGED);
            }

            preparationValidator.requirePolicyCoverage(
                    showtime.getPricingPolicy(),
                    showtime.getExperienceType(),
                    showtime.getContentFormat(),
                    showtime.getOfficialReleaseDate(),
                    request.startsAt(),
                    showtimeLayoutService.getSeatSnapshotsForGeneration(showtime));
        }

        validateNoOverlapExcludingId(
                showtime.getRoom().getId(), showtimeId, request.startsAt(), request.endsAt());

        showtime.changeSchedule(request.startsAt(), request.endsAt());

        showtimeRepository.flush();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse openForBooking(UUID showtimeId) {
        Showtime showtime = lockForMutation(showtimeId);

        validateRoomAvailable(showtime.getRoom());
        showtime.openForBooking();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse close(UUID showtimeId) {
        Showtime showtime = lockForMutation(showtimeId);
        showtime.close();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse cancel(UUID showtimeId) {
        Showtime showtime = lockForMutation(showtimeId);
        showtime.cancel();

        return showtimeMapper.toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse complete(UUID showtimeId) {
        Showtime showtime = lockForMutation(showtimeId);
        showtime.complete();

        return showtimeMapper.toResponse(showtime);
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

    private void validateRoomAvailable(Room room) {
        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        if (!room.getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }
    }

    private void validateNoOverlap(UUID roomId, OffsetDateTime startsAt, OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        if (!showtimeRepository
                .findOverlapsForRead(roomId, startsAt, endsAt, null, NON_BLOCKING_STATUSES)
                .isEmpty()) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }
    }

    private void validateNoOverlapExcludingId(
            UUID roomId, UUID showtimeId, OffsetDateTime startsAt, OffsetDateTime endsAt) {

        validateTimeRange(startsAt, endsAt);

        if (!showtimeRepository
                .findOverlapsForRead(roomId, startsAt, endsAt, showtimeId, NON_BLOCKING_STATUSES)
                .isEmpty()) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
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

    private Showtime lockForMutation(UUID showtimeId) {
        UUID roomId =
                showtimeRepository
                        .findRoomIdByShowtimeId(showtimeId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));

        UUID cinemaId =
                roomRepository
                        .findCinemaIdByRoomId(roomId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        cinemaRepository
                .findByIdForRead(cinemaId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        roomRepository
                .findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        return showtimeRepository
                .findByIdForUpdate(showtimeId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.SHOWTIME_NOT_FOUND));
    }
}
