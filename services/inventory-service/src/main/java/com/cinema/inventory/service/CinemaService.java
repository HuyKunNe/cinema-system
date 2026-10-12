package com.cinema.inventory.service;

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;

import java.util.List;
import java.util.UUID;

public interface CinemaService {

    CinemaResponse create(CreateCinemaRequest request);

    CinemaResponse getById(UUID cinemaId);

    List<CinemaResponse> getActiveCinemas(String city);

    List<CinemaResponse> getManagedCinemas(String city, Boolean active);

    CinemaResponse update(UUID cinemaId, UpdateCinemaRequest request);
}
