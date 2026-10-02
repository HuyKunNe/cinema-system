package com.cinema.movie.service.impl;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.repository.GenreRepository;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.service.MovieService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;
    private static final int MAX_TRAILER_URL_LENGTH = 500;

    public MovieServiceImpl(
            MovieRepository movieRepository,
            GenreRepository genreRepository,
            MovieMapper movieMapper) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.movieMapper = movieMapper;
    }

    @Override
    @Transactional
    public MovieResponse create(CreateMovieRequest request) {
        String normalizedTitle = request.title().trim();
        String normalizedTrailerUrl = normalizeTrailerUrl(request.trailerUrl());

        if (movieRepository.existsByTitleIgnoreCase(normalizedTitle)) {
            throw new ConflictException(MovieErrorCode.INVALID_MOVIE_TITLE);
        }

        Movie movie = movieMapper.toEntity(request);

        movie.setTitle(normalizedTitle);
        movie.setTrailerUrl(normalizedTrailerUrl);
        movie.setGenres(resolveGenres(request.genreIds()));

        Movie savedMovie = movieRepository.save(movie);

        return movieMapper.toResponse(savedMovie);
    }

    @Override
    public MovieResponse findById(UUID id) {
        return movieMapper.toResponse(findMovie(id));
    }

    @Override
    public List<MovieResponse> findAll() {
        return movieRepository.findAll().stream().map(movieMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public MovieResponse update(UUID id, UpdateMovieRequest request) {

        Movie movie = findMovie(id);

        String normalizedTitle = request.title().trim();
        String normalizedTrailerUrl = normalizeTrailerUrl(request.trailerUrl());

        if (movieRepository.existsByTitleIgnoreCaseAndIdNot(normalizedTitle, id)) {

            throw new ConflictException(MovieErrorCode.INVALID_MOVIE_TITLE);
        }

        movieMapper.updateEntity(request, movie);

        movie.setTitle(normalizedTitle);
        movie.setTrailerUrl(normalizedTrailerUrl);
        movie.setGenres(resolveGenres(request.genreIds()));

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Movie movie = findMovie(id);

        movieRepository.delete(movie);
    }

    private String normalizeTrailerUrl(String value) {
        if (value == null) {
            return null;
        }

        // Giữ cùng giới hạn đầu vào với @Size trong request DTO.
        if (value.length() > MAX_TRAILER_URL_LENGTH) {
            throw new ValidationException(MovieErrorCode.TRAILER_URL_TOO_LONG);
        }

        String normalized = value.trim();

        if (normalized.isBlank()) {
            return null;
        }

        URI uri;

        try {
            uri = new URI(normalized);
        } catch (URISyntaxException exception) {
            throw new ValidationException(MovieErrorCode.INVALID_TRAILER_URL);
        }

        boolean supportedScheme =
                "https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme());

        if (!uri.isAbsolute()
                || !supportedScheme
                || uri.getHost() == null
                || uri.getHost().isBlank()
                || uri.getPort() > 65535) {

            throw new ValidationException(MovieErrorCode.INVALID_TRAILER_URL);
        }

        return normalized;
    }

    private Movie findMovie(UUID id) {
        return movieRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND));
    }

    private Set<Genre> resolveGenres(Set<UUID> genreIds) {
        Set<UUID> uniqueGenreIds = new HashSet<>(genreIds);

        List<Genre> genres = genreRepository.findAllById(uniqueGenreIds);

        Set<UUID> foundIds = genres.stream().map(Genre::getId).collect(Collectors.toSet());

        Set<UUID> missingIds = new HashSet<>(uniqueGenreIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new NotFoundException(MovieErrorCode.GENRE_NOT_FOUND);
        }

        return new HashSet<>(genres);
    }
}
