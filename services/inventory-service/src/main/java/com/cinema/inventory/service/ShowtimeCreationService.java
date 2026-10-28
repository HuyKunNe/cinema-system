package com.cinema.inventory.service;

import com.cinema.common.exception.exception.BusinessException;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.client.MovieCatalogClient.MovieSchedulingDetails;
import com.cinema.inventory.dto.request.CreateShowtimeBatchRequest;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScheduling;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.exception.ShowtimeBatchItemException;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.CinemaPricingPolicyVersionRepository;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeBatchRequestRepository;
import com.cinema.inventory.repository.ShowtimeBatchRequestRepository.StoredBatch;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowtimeCreationService {

    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CinemaPricingPolicyVersionRepository pricingVersionRepository;
    private final ShowtimeBatchRequestRepository batchRequestRepository;
    private final ShowtimeBatchPayloadCodec batchCodec;
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
            CinemaPricingPolicyVersionRepository pricingVersionRepository,
            ShowtimeBatchRequestRepository batchRequestRepository,
            ShowtimeBatchPayloadCodec batchCodec) {

        this.roomRepository = roomRepository;
        this.showtimeRepository = showtimeRepository;
        this.layoutService = layoutService;
        this.seatGenerationService = seatGenerationService;
        this.mapper = mapper;
        this.movieCatalogClient = movieCatalogClient;
        this.clock = clock;
        this.pricingVersionRepository = pricingVersionRepository;
        this.batchRequestRepository = batchRequestRepository;
        this.batchCodec = batchCodec;

        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Transactional(propagation = Propagation.NEVER)
    public ShowtimeResponse create(CreateShowtimeRequest request) {
        PreparedCreation prepared = prepare(request, new HashMap<>(), new HashMap<>());

        return transactions.execute(
                status ->
                        persist(
                                prepared.request(),
                                prepared.screeningVersion(),
                                prepared.scheduling(),
                                prepared.releaseDate()));
    }

    @Transactional(propagation = Propagation.NEVER)
    public List<ShowtimeResponse> createBatch(CreateShowtimeBatchRequest request) {

        if (request == null
                || request.clientRequestId() == null
                || request.showtimes() == null
                || request.showtimes().isEmpty()
                || request.showtimes().size() > CreateShowtimeBatchRequest.MAX_SHOWTIMES
                || request.showtimes().stream().anyMatch(Objects::isNull)) {

            throw new ValidationException(InventoryErrorCode.INVALID_SHOWTIME_BATCH);
        }

        UUID clientRequestId = request.clientRequestId();
        List<CreateShowtimeRequest> items = List.copyOf(request.showtimes());

        String requestHash = batchCodec.fingerprint(items);

        var completed = batchRequestRepository.find(clientRequestId);

        if (completed.isPresent()) {
            StoredBatch stored = completed.get();
            requireSameRequest(stored, requestHash);
            return batchCodec.decode(stored.responseJson());
        }

        Map<UUID, MovieSchedulingDetails> movieCache = new HashMap<>();
        Map<ScreeningVersionKey, ShowtimeScreeningVersion> versionCache = new HashMap<>();

        List<PreparedCreation> prepared = new ArrayList<>(items.size());

        for (int index = 0; index < items.size(); index++) {
            try {
                prepared.add(prepare(items.get(index), movieCache, versionCache));

            } catch (BusinessException exception) {
                throw new ShowtimeBatchItemException(index, exception);
            }
        }

        return transactions.execute(
                status -> {
                    batchRequestRepository.claim(clientRequestId, requestHash);

                    StoredBatch stored = batchRequestRepository.findForUpdate(clientRequestId);

                    requireSameRequest(stored, requestHash);

                    if (stored.responseJson() != null) {
                        return batchCodec.decode(stored.responseJson());
                    }

                    lockBatchRooms(prepared);

                    List<ShowtimeResponse> responses = new ArrayList<>(prepared.size());

                    for (int index = 0; index < prepared.size(); index++) {
                        PreparedCreation item = prepared.get(index);

                        try {
                            responses.add(
                                    persist(
                                            item.request(),
                                            item.screeningVersion(),
                                            item.scheduling(),
                                            item.releaseDate()));

                        } catch (BusinessException exception) {
                            throw new ShowtimeBatchItemException(index, exception);
                        }
                    }

                    List<ShowtimeResponse> result = List.copyOf(responses);

                    batchRequestRepository.complete(
                            clientRequestId, requestHash, batchCodec.encode(result));

                    return result;
                });
    }

    private void requireSameRequest(StoredBatch stored, String requestHash) {

        if (!requestHash.equals(stored.requestHash())) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_BATCH_REQUEST_CONFLICT);
        }
    }

    private PreparedCreation prepare(
            CreateShowtimeRequest request,
            Map<UUID, MovieSchedulingDetails> movieCache,
            Map<ScreeningVersionKey, ShowtimeScreeningVersion> versionCache) {

        if (request == null
                || request.movieId() == null
                || request.movieScreeningVersionId() == null) {

            throw new ValidationException(InventoryErrorCode.SCREENING_VERSION_REQUIRED);
        }

        if (request.roomId() == null) {
            throw new ValidationException(InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }

        validateTimes(request.startsAt(), request.endsAt());
        ShowtimeScheduling.validateBreak(request.breakMinutes());

        MovieSchedulingDetails movie =
                movieCache.computeIfAbsent(
                        request.movieId(), movieCatalogClient::getMovieForScheduling);

        ScreeningVersionKey key =
                new ScreeningVersionKey(request.movieId(), request.movieScreeningVersionId());

        ShowtimeScreeningVersion cachedVersion =
                versionCache.computeIfAbsent(
                        key,
                        ignored ->
                                movieCatalogClient.getActiveVersion(
                                        key.movieId(), key.screeningVersionId()));

        ShowtimeScreeningVersion screeningVersion =
                new ShowtimeScreeningVersion(
                        cachedVersion.getId(),
                        cachedVersion.getVersion(),
                        cachedVersion.getContentFormat(),
                        cachedVersion.getAudioLanguage(),
                        cachedVersion.getPresentationType(),
                        cachedVersion.getSubtitleLanguage());

        ShowtimeScheduling scheduling =
                new ShowtimeScheduling(
                        movie.version(),
                        movie.durationMinutes(),
                        request.breakMinutes(),
                        request.endsAt());

        scheduling.validateDuration(request.startsAt(), request.endsAt());

        return new PreparedCreation(request, screeningVersion, scheduling, movie.releaseDate());
    }

    private void lockBatchRooms(List<PreparedCreation> prepared) {
        Map<UUID, Integer> firstIndexByRoom = new HashMap<>();

        for (int index = 0; index < prepared.size(); index++) {
            firstIndexByRoom.putIfAbsent(prepared.get(index).request().roomId(), index);
        }

        List<UUID> roomIds = firstIndexByRoom.keySet().stream().sorted().toList();

        for (UUID roomId : roomIds) {
            roomRepository
                    .findByIdForUpdate(roomId)
                    .orElseThrow(
                            () ->
                                    new ShowtimeBatchItemException(
                                            firstIndexByRoom.get(roomId),
                                            new NotFoundException(
                                                    InventoryErrorCode.ROOM_NOT_FOUND)));
        }
    }

    private record ScreeningVersionKey(UUID movieId, UUID screeningVersionId) {}

    private record PreparedCreation(
            CreateShowtimeRequest request,
            ShowtimeScreeningVersion screeningVersion,
            ShowtimeScheduling scheduling,
            LocalDate releaseDate) {}

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

        if (saved.getPricingPolicyVersion() != null) {
            seatGenerationService.generateFromPublishedPricing(saved);
        } else {
            seatGenerationService.validateForDeferredGeneration(saved);
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
