package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScheduling;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

@Service
public class ShowtimeCreationService {

    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaPricingPolicyVersionRepository pricingVersionRepository;
    private final ShowtimeLayoutService layoutService;
    private final ShowSeatGenerationService seatGenerationService;
    private final ShowtimeMapper mapper;
    private final MovieCatalogClient movieCatalogClient;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public ShowtimeCreationService(
            RoomRepository roomRepository,
            ShowtimeRepository showtimeRepository,
            ShowtimeLayoutService layoutService,
            ShowSeatGenerationService seatGenerationService,
            ShowtimeMapper mapper,
            MovieCatalogClient movieCatalogClient,
            Clock clock,
            PlatformTransactionManager transactionManager,
            CinemaPricingPolicyVersionRepository pricingVersionRepository) {

        this.roomRepository = roomRepository;
        this.showtimeRepository = showtimeRepository;
        this.layoutService = layoutService;
        this.seatGenerationService = seatGenerationService;
        this.mapper = mapper;
        this.movieCatalogClient = movieCatalogClient;
        this.clock = clock;
        this.pricingVersionRepository = pricingVersionRepository;

        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Transactional(propagation = Propagation.NEVER)
    public ShowtimeResponse create(CreateShowtimeRequest request) {
        if (request == null
                || request.movieId() == null
                || request.movieScreeningVersionId() == null) {
            throw new ValidationException(InventoryErrorCode.SCREENING_VERSION_REQUIRED);
        }

        if (request.roomId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }

        if (!request.isPricingSourceValid()) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PRICING);
        }

        validateTimes(request.startsAt(), request.endsAt());
        ShowtimeScheduling.validateBreak(request.breakMinutes());

        var movie = movieCatalogClient.getMovieForScheduling(request.movieId());

        ShowtimeScreeningVersion screeningVersion =
                movieCatalogClient.getActiveVersion(
                        request.movieId(), request.movieScreeningVersionId());

        ShowtimeScheduling scheduling =
                new ShowtimeScheduling(
                        movie.version(),
                        movie.durationMinutes(),
                        request.breakMinutes(),
                        request.endsAt());

        scheduling.validateDuration(request.startsAt(), request.endsAt());

        return transactions.execute(
                status -> persist(request, screeningVersion, scheduling, movie.releaseDate()));
    }

    private ShowtimeResponse persist(
            CreateShowtimeRequest request,
            ShowtimeScreeningVersion screeningVersion,
            ShowtimeScheduling scheduling,
            LocalDate releaseDate) {

        Room room =
                roomRepository
                        .findByIdForUpdate(request.roomId())
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.ROOM_NOT_FOUND));

        validateTimes(request.startsAt(), request.endsAt());

        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        if (!room.getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }

        ShowtimeReleaseDateValidator.validate(
                request.startsAt(), room.getCinema().getTimeZone(), releaseDate);

        var formats = room.getSupportedContentFormats();

        if (formats.isEmpty()) {
            throw new ConflictException(InventoryErrorCode.ROOM_CONTENT_FORMATS_NOT_CONFIGURED);
        }

        if (!formats.contains(screeningVersion.getContentFormat())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_FORMAT_NOT_SUPPORTED);
        }

        var layout = layoutService.resolveForNewShowtime(room.getId(), request.roomLayoutId());

        boolean conflict =
                showtimeRepository.existsSchedulingConflict(
                        room.getId(),
                        null,
                        request.startsAt(),
                        scheduling.getReadyAt(),
                        Set.of(ShowtimeStatus.CANCELLED));

        if (conflict) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }

        Showtime showtime =
                new Showtime(
                        request.movieId(),
                        room,
                        request.startsAt(),
                        request.endsAt(),
                        layout,
                        screeningVersion,
                        scheduling);

        if (request.pricingPolicyVersionId() != null) {
            var selected =
                    pricingVersionRepository
                            .findById(request.pricingPolicyVersionId())
                            .orElseThrow(
                                    () ->
                                            new NotFoundException(
                                                    InventoryErrorCode
                                                            .PRICING_POLICY_VERSION_NOT_FOUND));

            showtime.replacePricingPolicyVersion(selected);
        }

        Showtime saved = showtimeRepository.save(showtime);

        if (saved.getPricingPolicyVersion() == null) {
            seatGenerationService.generate(saved, request.basePrice());
        } else {
            seatGenerationService.generateFromPublishedPricing(saved);
        }

        showtimeRepository.flush();

        return mapper.toResponse(saved);
    }

    private void validateTimes(OffsetDateTime startsAt, OffsetDateTime endsAt) {

        if (startsAt == null || endsAt == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        if (!endsAt.isAfter(startsAt)) {
            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_PERIOD);
        }

        if (!startsAt.isAfter(OffsetDateTime.now(clock))) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_START_NOT_FUTURE);
        }
    }
}
