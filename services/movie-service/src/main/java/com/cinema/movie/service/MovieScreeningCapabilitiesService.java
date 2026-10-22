package com.cinema.movie.service;

import com.cinema.common.api.screening.ReplaceScreeningCapabilitiesRequest;
import com.cinema.common.api.screening.ScreeningFormat;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.movie.dto.response.MovieScreeningCapabilitiesResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.repository.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MovieScreeningCapabilitiesService {

    private final MovieRepository movieRepository;

    public MovieScreeningCapabilitiesService(
            MovieRepository movieRepository) {

        this.movieRepository = movieRepository;
    }

    @Transactional
    public MovieScreeningCapabilitiesResponse get(UUID movieId) {
        Movie movie =
                movieRepository.findByIdForScreeningRead(movieId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        MovieErrorCode.MOVIE_NOT_FOUND));

        return response(movie);
    }

    @Transactional
    public MovieScreeningCapabilitiesResponse replace(
            UUID movieId,
            ReplaceScreeningCapabilitiesRequest request) {

        Movie movie =
                movieRepository.findByIdForScreeningUpdate(movieId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        MovieErrorCode.MOVIE_NOT_FOUND));

        if (!movie.getVersion().equals(request.expectedVersion())) {
            throw new ConflictException(
                    MovieErrorCode.SCREENING_CAPABILITY_VERSION_CONFLICT);
        }

        movie.replaceScreeningFormats(request.formats());
        movieRepository.flush();

        return response(movie);
    }

    private MovieScreeningCapabilitiesResponse response(Movie movie) {
        return new MovieScreeningCapabilitiesResponse(
                movie.getId(),
                movie.getVersion(),
                movie.getReleaseDate(),
                movie.getStatus(),
                ScreeningFormat.options(movie.getScreeningFormats()),
                ScreeningFormat.allOptions());
    }
}
