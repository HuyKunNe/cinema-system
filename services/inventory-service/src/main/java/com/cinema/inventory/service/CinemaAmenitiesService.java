package com.cinema.inventory.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.ReplaceCinemaAmenitiesRequest;
import com.cinema.inventory.dto.response.CinemaAmenitiesResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaAmenitiesMapper;
import com.cinema.inventory.repository.CinemaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CinemaAmenitiesService {

    private final CinemaRepository cinemaRepository;
    private final CinemaAmenitiesMapper mapper;

    public CinemaAmenitiesService(CinemaRepository cinemaRepository, CinemaAmenitiesMapper mapper) {

        this.cinemaRepository = cinemaRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CinemaAmenitiesResponse get(UUID cinemaId) {
        Cinema cinema =
                cinemaRepository
                        .findById(cinemaId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        return mapper.toResponse(cinema);
    }

    @Transactional
    public CinemaAmenitiesResponse replace(UUID cinemaId, ReplaceCinemaAmenitiesRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || request.amenities() == null) {

            throw new ValidationException(InventoryErrorCode.CINEMA_AMENITIES_INVALID);
        }

        Cinema cinema =
                cinemaRepository
                        .findByIdForUpdate(cinemaId)
                        .orElseThrow(
                                () -> new NotFoundException(InventoryErrorCode.CINEMA_NOT_FOUND));

        if (!request.expectedVersion().equals(cinema.getVersion())) {
            throw new ConflictException(InventoryErrorCode.CINEMA_VERSION_CONFLICT);
        }

        cinema.replaceAmenities(request.amenities());

        cinemaRepository.flush();

        return mapper.toResponse(cinema);
    }
}
