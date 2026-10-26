package com.cinema.inventory.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.service.CinemaService;

@Service
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper cinemaMapper;
    private final Clock clock;

    public CinemaServiceImpl(
            CinemaRepository cinemaRepository, CinemaMapper cinemaMapper, Clock clock) {

        this.cinemaRepository = cinemaRepository;
        this.cinemaMapper = cinemaMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CinemaResponse create(CreateCinemaRequest request) {
        Cinema cinema =
                new Cinema(
                        normalize(request.name()),
                        normalize(request.address()),
                        normalize(request.city()));

        cinema.updatePublicDetails(
                request.phone(),
                request.email(),
                request.openingHours(),
                request.directions(),
                request.timeZone());

        Cinema savedCinema = cinemaRepository.save(cinema);

        return cinemaMapper.toResponse(savedCinema);
    }

    @Override
    @Transactional(readOnly = true)
    public CinemaResponse getById(UUID cinemaId) {
        return cinemaMapper.toResponse(findCinema(cinemaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getActiveCinemas(String city) {
        List<Cinema> cinemas;

        if (StringUtils.hasText(city)) {
            cinemas =
                    cinemaRepository.findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                            city.trim());
        } else {
            cinemas = cinemaRepository.findAllByActiveTrueOrderByNameAsc();
        }

        return cinemaMapper.toResponses(cinemas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CinemaResponse> getManagedCinemas(String city, Boolean active) {
        List<Cinema> cinemas;

        if (StringUtils.hasText(city)) {
            String normalizedCity = city.trim();

            cinemas =
                    active == null
                            ? cinemaRepository.findAllByCityIgnoreCaseOrderByNameAscIdAsc(
                                    normalizedCity)
                            : cinemaRepository.findAllByCityIgnoreCaseAndActiveOrderByNameAscIdAsc(
                                    normalizedCity, active);
        } else {
            cinemas =
                    active == null
                            ? cinemaRepository.findAllByOrderByNameAscIdAsc()
                            : cinemaRepository.findAllByActiveOrderByNameAscIdAsc(active);
        }

        return cinemaMapper.toResponses(cinemas);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CinemaResponse update(UUID cinemaId, UpdateCinemaRequest request) {

        Cinema cinema =
                cinemaRepository
                        .findByIdForUpdate(cinemaId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        if (!request.active()) {
            String effectiveTimeZone =
                    request.timeZone() == null ? cinema.getTimeZone() : request.timeZone().trim();

            OffsetDateTime from = startOfTodayAtCinema(effectiveTimeZone);

            if (cinemaRepository.existsShowtimeWithStatusStartingAtOrAfter(
                    cinemaId, ShowtimeStatus.OPEN_FOR_BOOKING, from)) {

                throw new ConflictException(InventoryErrorCode.CINEMA_DEACTIVATION_BLOCKED);
            }
        }

        cinema.update(
                normalize(request.name()), normalize(request.address()), normalize(request.city()));

        cinema.changeActive(request.active());

        cinema.updatePublicDetails(
                request.phone(),
                request.email(),
                request.openingHours(),
                request.directions(),
                request.timeZone());

        cinemaRepository.flush();

        return cinemaMapper.toResponse(cinema);
    }

    private Cinema findCinema(UUID cinemaId) {
        return cinemaRepository
                .findById(cinemaId)
                .orElseThrow(() -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));
    }

    private OffsetDateTime startOfTodayAtCinema(String configuredZone) {

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
