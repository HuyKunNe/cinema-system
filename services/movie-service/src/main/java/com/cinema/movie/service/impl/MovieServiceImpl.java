package com.cinema.movie.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
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

@Service
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MovieMapper movieMapper;

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

        if (movieRepository.existsByTitleIgnoreCase(normalizedTitle)) {
            throw new ConflictException(MovieErrorCode.INVALID_MOVIE_TITLE);
        }

        Movie movie = movieMapper.toEntity(request);

        movie.setTitle(normalizedTitle);
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
        return movieRepository.findAll()
                .stream()
                .map(movieMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MovieResponse update(
            UUID id,
            UpdateMovieRequest request) {
        Movie movie = findMovie(id);
        String normalizedTitle = request.title().trim();

        if (movieRepository.existsByTitleIgnoreCaseAndIdNot(
                normalizedTitle,
                id)) {
            throw new ConflictException(MovieErrorCode.INVALID_MOVIE_TITLE);
        }

        movieMapper.updateEntity(request, movie);

        movie.setTitle(normalizedTitle);
        movie.setGenres(resolveGenres(request.genreIds()));

        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Movie movie = findMovie(id);

        movieRepository.delete(movie);
    }

    private Movie findMovie(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND));
    }

    private Set<Genre> resolveGenres(Set<UUID> genreIds) {
        Set<UUID> uniqueGenreIds = new HashSet<>(genreIds);

        List<Genre> genres = genreRepository.findAllById(uniqueGenreIds);

        Set<UUID> foundIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<UUID> missingIds = new HashSet<>(uniqueGenreIds);
        missingIds.removeAll(foundIds);

        if (!missingIds.isEmpty()) {
            throw new NotFoundException(MovieErrorCode.GENRE_NOT_FOUND);
        }

        return new HashSet<>(genres);
    }
}
