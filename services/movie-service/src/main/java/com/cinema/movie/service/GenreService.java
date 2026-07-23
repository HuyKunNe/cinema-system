package com.cinema.movie.service;

import java.util.List;
import java.util.UUID;

import com.cinema.movie.dto.request.CreateGenreRequest;
import com.cinema.movie.dto.request.UpdateGenreRequest;
import com.cinema.movie.dto.response.GenreResponse;

public interface GenreService {

    GenreResponse create(CreateGenreRequest request);

    GenreResponse findById(UUID id);

    List<GenreResponse> findAll();

    GenreResponse update(UUID id, UpdateGenreRequest request);

    void delete(UUID id);
}
