package com.cinema.movie.service;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.movie.dto.request.CreateMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.SaveMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.UpdateMovieScreeningVersionActiveRequest;
import com.cinema.movie.dto.response.MovieScreeningVersionResponse;
import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieScreeningVersion;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;
import com.cinema.movie.error.MovieErrorCode;
import com.cinema.movie.mapper.MovieScreeningVersionMapper;
import com.cinema.movie.repository.MovieRepository;
import com.cinema.movie.repository.MovieScreeningVersionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
                        request.projectionTechnology(),
                        request.audioLanguage(),
                        request.presentationType(),
                        request.subtitleLanguage());

        var existing =
                versionRepository
                        .findByMovie_IdAndContentFormatAndProjectionTechnologyAndAudioLanguageAndPresentationTypeAndSubtitleLanguage(
                                movieId,
                                candidate.getContentFormat(),
                                candidate.getProjectionTechnology(),
                                candidate.getAudioLanguage(),
                                candidate.getPresentationType(),
                                Objects.toString(candidate.getSubtitleLanguage(), ""));

        if (existing.isPresent()) {
            throw new ConflictException(MovieErrorCode.SCREENING_VERSION_ALREADY_EXISTS);
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
            throw new ValidationException(MovieErrorCode.SCREENING_VERSIONS_REQUIRED);
        }

        Movie movie = lockMovie(movieId);
        List<MovieScreeningVersion> persistedVersions = versionRepository.findAllForUpdate(movieId);
        Map<UUID, MovieScreeningVersion> persistedById =
                persistedVersions.stream()
                        .collect(Collectors.toMap(MovieScreeningVersion::getId, version -> version));

        Set<UUID> requestedIds = new HashSet<>();
        Set<ScreeningVersionIdentity> requestedIdentities = new HashSet<>();
        List<PreparedEdit> preparedEdits = new ArrayList<>(requests.size());

        for (SaveMovieScreeningVersionRequest request : requests) {
            if (request == null || request.active() == null) {
                throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
            }

            MovieScreeningVersion candidate =
                    new MovieScreeningVersion(
                            movie,
                            request.contentFormat(),
                            request.projectionTechnology(),
                            request.audioLanguage(),
                            request.presentationType(),
                            request.subtitleLanguage());

            if (!requestedIdentities.add(ScreeningVersionIdentity.from(candidate))) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_ALREADY_EXISTS);
            }

            if (request.id() == null) {
                if (request.expectedVersion() != null) {
                    throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
                }

                preparedEdits.add(new PreparedEdit(request, candidate, null));
                continue;
            }

            if (request.expectedVersion() == null
                    || request.expectedVersion() < 0
                    || !requestedIds.add(request.id())) {

                throw new ValidationException(MovieErrorCode.INVALID_SCREENING_VERSION);
            }

            MovieScreeningVersion existing = persistedById.get(request.id());

            if (existing == null) {
                throw new NotFoundException(MovieErrorCode.SCREENING_VERSION_NOT_FOUND);
            }

            if (!request.expectedVersion().equals(existing.getVersion())) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_CONFLICT);
            }

            if (!sameDefinition(existing, candidate)) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_IMMUTABLE);
            }

            preparedEdits.add(new PreparedEdit(request, candidate, existing));
        }

        if (!requestedIds.equals(persistedById.keySet())) {
            throw new ValidationException(MovieErrorCode.SCREENING_VERSION_SET_INCOMPLETE);
        }

        for (PreparedEdit edit : preparedEdits) {
            if (edit.existing() != null) {
                edit.existing().changeActive(edit.request().active());
                continue;
            }

            MovieScreeningVersion candidate = edit.candidate();

            boolean duplicate =
                    versionRepository
                            .findByMovie_IdAndContentFormatAndProjectionTechnologyAndAudioLanguageAndPresentationTypeAndSubtitleLanguage(
                                    movieId,
                                    candidate.getContentFormat(),
                                    candidate.getProjectionTechnology(),
                                    candidate.getAudioLanguage(),
                                    candidate.getPresentationType(),
                                    Objects.toString(candidate.getSubtitleLanguage(), ""))
                            .isPresent();

            if (duplicate) {
                throw new ConflictException(MovieErrorCode.SCREENING_VERSION_ALREADY_EXISTS);
            }

            candidate.changeActive(edit.request().active());
            versionRepository.saveAndFlush(candidate);
        }

        versionRepository.flush();
    }

    private static boolean sameDefinition(
            MovieScreeningVersion existing, MovieScreeningVersion candidate) {

        return existing.getContentFormat() == candidate.getContentFormat()
                && existing.getProjectionTechnology() == candidate.getProjectionTechnology()
                && existing.getPresentationType() == candidate.getPresentationType()
                && existing.getAudioLanguage().equals(candidate.getAudioLanguage())
                && Objects.equals(existing.getSubtitleLanguage(), candidate.getSubtitleLanguage());
    }

    private Movie lockMovie(UUID movieId) {
        return movieRepository
                .findByIdForUpdate(movieId)
                .orElseThrow(() -> new NotFoundException(MovieErrorCode.MOVIE_NOT_FOUND));
    }

    private record PreparedEdit(
            SaveMovieScreeningVersionRequest request,
            MovieScreeningVersion candidate,
            MovieScreeningVersion existing) {}

    private record ScreeningVersionIdentity(
            ContentFormat contentFormat,
            ProjectionTechnology projectionTechnology,
            String audioLanguage,
            PresentationType presentationType,
            String subtitleLanguage) {

        private static ScreeningVersionIdentity from(MovieScreeningVersion version) {
            return new ScreeningVersionIdentity(
                    version.getContentFormat(),
                    version.getProjectionTechnology(),
                    version.getAudioLanguage().toLowerCase(Locale.ROOT),
                    version.getPresentationType(),
                    Objects.toString(version.getSubtitleLanguage(), "").toLowerCase(Locale.ROOT));
        }
    }

    public record CreateResult(boolean created, MovieScreeningVersionResponse response) {}
}
