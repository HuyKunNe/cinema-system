package com.cinema.movie.service;

import java.util.List;
import java.util.UUID;

import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;

public interface MovieService {

    MovieResponse create(CreateMovieRequest request);

    MovieResponse findById(UUID id);

    List<MovieResponse> findAll();

    MovieResponse update(
            UUID id,
            UpdateMovieRequest request);

    void delete(UUID id);
}
