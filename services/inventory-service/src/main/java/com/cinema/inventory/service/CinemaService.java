package com.cinema.inventory.service;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;

public interface CinemaService {

    CinemaResponse create(CreateCinemaRequest request);

    CinemaResponse getById(UUID cinemaId);

    List<CinemaResponse> getActiveCinemas(String city);

    CinemaResponse update(
            UUID cinemaId,
            UpdateCinemaRequest request);
}