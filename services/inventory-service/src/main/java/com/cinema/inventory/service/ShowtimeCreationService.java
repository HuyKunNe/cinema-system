package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.client.MovieCatalogClient;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.entity.ShowtimeScreeningVersion;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.ShowtimeMapper;
import com.cinema.inventory.repository.RoomRepository;
import com.cinema.inventory.repository.ShowtimeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

@Service
public class ShowtimeCreationService {

    private final RoomRepository roomRepository;
    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeLayoutService layoutService;
    private final ShowSeatGenerationService seatGenerationService;
    private final ShowtimeMapper mapper;
    private final MovieCatalogClient movieCatalogClient;
    private final TransactionTemplate transactions;

    public ShowtimeCreationService(
            RoomRepository roomRepository,
            ShowtimeRepository showtimeRepository,
            ShowtimeLayoutService layoutService,
            ShowSeatGenerationService seatGenerationService,
            ShowtimeMapper mapper,
            MovieCatalogClient movieCatalogClient,
            PlatformTransactionManager transactionManager) {

        this.roomRepository = roomRepository;
        this.showtimeRepository = showtimeRepository;
        this.layoutService = layoutService;
        this.seatGenerationService = seatGenerationService;
        this.mapper = mapper;
        this.movieCatalogClient = movieCatalogClient;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(propagation = Propagation.NEVER)
    public ShowtimeResponse create(CreateShowtimeRequest request) {
        if (request == null
                || request.movieId() == null
                || request.movieScreeningVersionId() == null) {
            throw new ValidationException(
                    InventoryErrorCode.SCREENING_VERSION_REQUIRED);
        }

        if (request.roomId() == null) {
            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_ROOM_REQUIRED);
        }

        if (request.startsAt() == null || request.endsAt() == null) {
            throw new ValidationException(
                    InventoryErrorCode.SHOWTIME_PERIOD_REQUIRED);
        }

        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new ValidationException(
                    InventoryErrorCode.INVALID_SHOWTIME_PERIOD);
        }

        ShowtimeScreeningVersion snapshot =
                movieCatalogClient.getActiveVersion(
                        request.movieId(), request.movieScreeningVersionId());

        return transactions.execute(status -> persist(request, snapshot));
    }

    private ShowtimeResponse persist(
            CreateShowtimeRequest request,
            ShowtimeScreeningVersion snapshot) {

        Room room =
                roomRepository.findByIdForUpdate(request.roomId())
                        .orElseThrow(
                                () -> new NotFoundException(
                                        InventoryErrorCode.ROOM_NOT_FOUND));

        if (!room.isActive()) {
            throw new ConflictException(InventoryErrorCode.ROOM_INACTIVE);
        }

        if (!room.getCinema().isActive()) {
            throw new ConflictException(InventoryErrorCode.CINEMA_INACTIVE);
        }

        var formats = room.getSupportedContentFormats();

        if (formats.isEmpty()) {
            throw new ConflictException(
                    InventoryErrorCode.ROOM_CONTENT_FORMATS_NOT_CONFIGURED);
        }

        if (!formats.contains(snapshot.getContentFormat())) {
            throw new ConflictException(
                    InventoryErrorCode.SHOWTIME_FORMAT_NOT_SUPPORTED);
        }

        var layout =
                layoutService.resolveForNewShowtime(
                        room.getId(), request.roomLayoutId());

        boolean overlapping =
                showtimeRepository.existsOverlappingShowtime(
                        room.getId(),
                        request.startsAt(),
                        request.endsAt(),
                        Set.of(ShowtimeStatus.CANCELLED));

        if (overlapping) {
            throw new ConflictException(InventoryErrorCode.SHOWTIME_OVERLAP);
        }

        Showtime showtime =
                new Showtime(
                        request.movieId(),
                        room,
                        request.startsAt(),
                        request.endsAt(),
                        layout,
                        snapshot);

        Showtime saved = showtimeRepository.save(showtime);

        seatGenerationService.generate(saved, request.basePrice());
        showtimeRepository.flush();

        return mapper.toResponse(saved);
    }
}
