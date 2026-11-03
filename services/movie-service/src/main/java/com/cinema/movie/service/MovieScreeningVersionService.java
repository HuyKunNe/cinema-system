package com.cinema.movie.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.movie.dto.request.CreateMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.SaveMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.UpdateMovieScreeningVersionActiveRequest;
import com.cinema.movie.dto.response.MovieScreeningVersionResponse;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieScreeningVersion;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.mapper.MovieScreeningVersionMapper;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.MovieScreeningVersionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class MovieScreeningVersionService {

    private final MovieRepository movieRepository;
    private final MovieScreeningVersionRepository versionRepository;
    private final MovieScreeningVersionMapper mapper;

    public MovieScreeningVersionService(
            MovieRepository movieRepository,
            MovieScreeningVersionRepository versionRepository,
            MovieScreeningVersionMapper mapper) {

        this.movieRepository = movieRepository;
        this.versionRepository = versionRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<MovieScreeningVersionResponse> list(UUID movieId, Boolean active) {

        if (!movieRepository.existsById(movieId)) {
            throw new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND);
        }

        List<MovieScreeningVersion> versions =
                active == null
                        ? versionRepository.findAllByMovie_IdOrderByCreatedAtAscIdAsc(movieId)
                        : versionRepository.findAllByMovie_IdAndActiveOrderByCreatedAtAscIdAsc(
                                movieId, active);

        return versions.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MovieScreeningVersionResponse get(UUID movieId, UUID versionId) {

        MovieScreeningVersion version =
                versionRepository
                        .findByIdAndMovie_Id(versionId, movieId)
                        .orElseThrow(() ->
                                new NotFoundException(MovieErrorCode.SCREENING_VERSION_NOT_FOUND));

        return mapper.toResponse(version);
    }

    @Transactional
    public CreateResult create(UUID movieId, CreateMovieScreeningVersionRequest request) {

        if (request == null) {
            throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
        }

        Movie movie = lockMovie(movieId);

        MovieScreeningVersion candidate =
                new MovieScreeningVersion(
                        movie,
                        request.contentFormat(),
                        request.audioLanguage(),
                        request.presentationType(),
                        request.subtitleLanguage());

        var existing =
                versionRepository
                        .findByMovie_IdAndContentFormatAndAudioLanguageAndPresentationTypeAndSubtitleLanguage(
                                movieId,
                                candidate.getContentFormat(),
                                candidate.getAudioLanguage(),
                                candidate.getPresentationType(),
                                Objects.toString(candidate.getSubtitleLanguage(), ""));

        if (existing.isPresent()) {
            return new CreateResult(false, mapper.toResponse(existing.get()));
        }

        MovieScreeningVersion saved = versionRepository.saveAndFlush(candidate);

        return new CreateResult(true, mapper.toResponse(saved));
    }

    @Transactional
    public MovieScreeningVersionResponse updateActive(
            UUID movieId, UUID versionId, UpdateMovieScreeningVersionActiveRequest request) {

        if (request == null
                || request.expectedVersion() == null
                || request.expectedVersion() < 0
                || request.active() == null) {

            throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
        }

        lockMovie(movieId);

        MovieScreeningVersion version =
                versionRepository
                        .findForUpdate(movieId, versionId)
                        .orElseThrow(() ->
                                new NotFoundException(MovieErrorCode.SCREENING_VERSION_NOT_FOUND));

        if (!request.expectedVersion().equals(version.getVersion())) {
            throw new ConflictException(MovieErrorCode.SCREENING_VERSION_CONFLICT);
        }

        version.changeActive(request.active());

        versionRepository.flush();

        return mapper.toResponse(version);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void applyMovieEdits(UUID movieId, List<SaveMovieScreeningVersionRequest> requests) {

        if (requests == null || requests.isEmpty()) {
            return;
        }

        Movie movie = lockMovie(movieId);
        Set<UUID> requestedIds = new HashSet<>();

        for (SaveMovieScreeningVersionRequest request : requests) {
            if (request == null || request.active() == null) {
                throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
            }

            MovieScreeningVersion candidate =
                    new MovieScreeningVersion(
                            movie,
                            request.contentFormat(),
                            request.audioLanguage(),
                            request.presentationType(),
                            request.subtitleLanguage());

            if (request.id() == null) {
                if (request.expectedVersion() != null) {
                    throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
                }

                var existing =
                        versionRepository
                                .findByMovie_IdAndContentFormatAndAudioLanguageAndPresentationTypeAndSubtitleLanguage(
                                        movieId,
                                        candidate.getContentFormat(),
                                        candidate.getAudioLanguage(),
                                        candidate.getPresentationType(),
                                        Objects.toString(candidate.getSubtitleLanguage(), ""));

                if (existing.isPresent()) {
                    if (existing.get().isActive() != request.active()) {
                        throw new ConflictException(MovieErrorCode.SCREENING_VERSION_CONFLICT);
                    }

                    continue;
                }

                candidate.changeActive(request.active());
                versionRepository.save(candidate);
                continue;
            }

            if (request.expectedVersion() == null
                    || request.expectedVersion() < 0
                    || !requestedIds.add(request.id())) {

                throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
            }

            MovieScreeningVersion existing =
                    versionRepository
                            .findForUpdate(movieId, request.id())
                            .orElseThrow(() ->
                                    new NotFoundException(
                                            MovieErrorCode.SCREENING_VERSION_NOT_FOUND));

            if (!request.expectedVersion().equals(existing.getVersion())) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_CONFLICT);
            }

            if (!sameDefinition(existing, candidate)) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_IMMUTABLE);
            }

            existing.changeActive(request.active());
        }

        versionRepository.flush();
    }

    private static boolean sameDefinition(
            MovieScreeningVersion existing, MovieScreeningVersion candidate) {

        return existing.getContentFormat() == candidate.getContentFormat()
                && existing.getPresentationType() == candidate.getPresentationType()
                && existing.getAudioLanguage().equals(candidate.getAudioLanguage())
                && Objects.equals(existing.getSubtitleLanguage(), candidate.getSubtitleLanguage());
    }

    private Movie lockMovie(UUID movieId) {
        return movieRepository
                .findByIdForUpdate(movieId)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND));
    }

    public record CreateResult(boolean created, MovieScreeningVersionResponse response) {}
}
