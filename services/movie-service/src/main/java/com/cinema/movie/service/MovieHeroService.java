package com.cinema.movie.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.movie.config.MovieHeroProperties;
import com.cinema.movie.dto.request.UpdateMovieHeroRequest;
import com.cinema.movie.dto.response.MovieHeroConfigurationResponse;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.mapper.MovieMapper;
import com.cinema.movie.repository.MovieRepository;

@Service
@Transactional(readOnly = true)
public class MovieHeroService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieHeroProperties properties;
    private final Clock clock;

    public MovieHeroService(
            MovieRepository movieRepository,
            MovieMapper movieMapper,
            MovieHeroProperties properties,
            Clock clock) {

        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
        this.properties = properties;
        this.clock = clock;
    }

    public List<MovieResponse> findHero(Integer limit, List<UUID> movieIds) {
        int requestedLimit = limit == null ? properties.getDefaultLimit() : limit;

        if (requestedLimit < 1 || requestedLimit > properties.getMaxLimit()) {
            throw new ValidationException(MovieErrorCode.INVALID_HERO_QUERY);
        }

        if (movieIds != null
                && (movieIds.size() > properties.getMaxMovieIds()
                        || movieIds.stream().anyMatch(id -> id == null))) {

            throw new ValidationException(MovieErrorCode.INVALID_HERO_QUERY);
        }

        // A supplied empty filter must not become an unrestricted query.
        if (movieIds != null && movieIds.isEmpty()) {
            return List.of();
        }

        OffsetDateTime now =
                OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);

        PageRequest page = PageRequest.of(0, requestedLimit);

        List<UUID> selectedIds =
                movieIds == null
                        ? movieRepository.findHeroMovieIds(
                                MovieStatus.NOW_SHOWING, now, page)
                        : movieRepository.findHeroMovieIdsByMovieIds(
                                MovieStatus.NOW_SHOWING,
                                now,
                                movieIds.stream().distinct().toList(),
                                page);

        if (selectedIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Movie> moviesById =
                movieRepository.findAllWithGenresByIdIn(selectedIds).stream()
                        .collect(Collectors.toMap(Movie::getId, movie -> movie));

        // Restore the editorial order after the batch entity query.
        return selectedIds.stream()
                .map(id -> {
                    Movie movie = moviesById.get(id);

                    if (movie == null) {
                        throw new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND);
                    }

                    return movieMapper.toResponse(movie);
                })
                .toList();
    }

    @Transactional
    public MovieHeroConfigurationResponse updateHero(
            UUID movieId, UpdateMovieHeroRequest request) {

        if (!request.hasChanges()) {
            throw new ValidationException(MovieErrorCode.INVALID_HERO_CONFIGURATION);
        }

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND));

        boolean enabled = movie.isHeroEnabled();
        int priority = movie.getHeroPriority();

        if (request.hasField("heroEnabled")) {
            if (request.getHeroEnabled() == null) {
                throw new ValidationException(MovieErrorCode.INVALID_HERO_CONFIGURATION);
            }

            enabled = request.getHeroEnabled();
        }

        if (request.hasField("heroPriority")) {
            if (request.getHeroPriority() == null || request.getHeroPriority() < 0) {
                throw new ValidationException(MovieErrorCode.INVALID_HERO_CONFIGURATION);
            }

            priority = request.getHeroPriority();
        }

        OffsetDateTime startsAt =
                request.hasField("heroStartsAt")
                        ? normalizeTime(request.getHeroStartsAt())
                        : movie.getHeroStartsAt();

        OffsetDateTime endsAt =
                request.hasField("heroEndsAt")
                        ? normalizeTime(request.getHeroEndsAt())
                        : movie.getHeroEndsAt();

        // Validate the resulting configuration, including unchanged values.
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new ValidationException(MovieErrorCode.INVALID_HERO_CONFIGURATION);
        }

        movie.setHeroEnabled(enabled);
        movie.setHeroPriority(priority);
        movie.setHeroStartsAt(startsAt);
        movie.setHeroEndsAt(endsAt);

        Movie saved = movieRepository.saveAndFlush(movie);

        return new MovieHeroConfigurationResponse(
                saved.getId(),
                saved.isHeroEnabled(),
                saved.getHeroPriority(),
                saved.getHeroStartsAt(),
                saved.getHeroEndsAt());
    }

    private OffsetDateTime normalizeTime(OffsetDateTime value) {
        return value == null
                ? null
                : value.withOffsetSameInstant(ZoneOffset.UTC)
                        .truncatedTo(ChronoUnit.MICROS);
    }
}
