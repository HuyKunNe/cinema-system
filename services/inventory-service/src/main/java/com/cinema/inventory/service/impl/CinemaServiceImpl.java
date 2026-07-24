package com.cinema.inventory.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.exception.InventoryErrorCode;
import com.cinema.inventory.mapper.CinemaMapper;
import com.cinema.inventory.repository.CinemaRepository;
import com.cinema.inventory.service.CinemaService;

@Service
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;
    private final CinemaMapper cinemaMapper;

    public CinemaServiceImpl(
            CinemaRepository cinemaRepository,
            CinemaMapper cinemaMapper) {
        this.cinemaRepository = cinemaRepository;
        this.cinemaMapper = cinemaMapper;
    }

    @Override
    @Transactional
    public CinemaResponse create(CreateCinemaRequest request) {
        Cinema cinema = new Cinema(
                normalize(request.name()),
                normalize(request.address()),
                normalize(request.city()));

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
            cinemas = cinemaRepository
                    .findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(
                            city.trim());
        } else {
            cinemas = cinemaRepository
                    .findAllByActiveTrueOrderByNameAsc();
        }

        return cinemaMapper.toResponses(cinemas);
    }

    @Override
    @Transactional
    public CinemaResponse update(
            UUID cinemaId,
            UpdateCinemaRequest request) {
        Cinema cinema = findCinema(cinemaId);

        cinema.update(
                normalize(request.name()),
                normalize(request.address()),
                normalize(request.city()));

        cinema.changeActive(request.active());

        return cinemaMapper.toResponse(cinema);
    }

    private Cinema findCinema(UUID cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new NotFoundException(
                        InventoryErrorCode.CINEMA_NOT_FOUND));
    }

    private String normalize(String value) {
        return value.trim();
    }
}