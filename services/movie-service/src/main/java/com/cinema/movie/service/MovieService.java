package com.cinema.movie.service;

import com.cinema.common.response.model.PageResponse;
import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.MovieCatalogSort;
import com.cinema.movie.dto.request.UpdateMovieMetadataRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.MovieStatus;

import java.util.List;
import java.util.UUID;

public interface MovieService {

    MovieResponse create(CreateMovieRequest request);

    MovieResponse findById(UUID id);

    List<MovieResponse> findAll();

    PageResponse<MovieResponse> findCatalog(
            MovieStatus status, UUID genreId, int page, Integer size);

    PageResponse<MovieResponse> findCatalog(
            MovieStatus status, UUID genreId, int page, Integer size, List<UUID> movieIds);

    PageResponse<MovieResponse> findCatalog(
            MovieStatus status,
            UUID genreId,
            int page,
            Integer size,
            List<UUID> movieIds,
            MovieCatalogSort sort);

    MovieResponse update(UUID id, UpdateMovieRequest request);

    MovieResponse updateMetadata(UUID id, UpdateMovieMetadataRequest request);

    void delete(UUID id);
}
