package com.cinema.movie.repository;

import com.cinema.movie.entity.ContentFormat;
import com.cinema.movie.entity.MovieScreeningVersion;
import com.cinema.movie.entity.PresentationType;
import com.cinema.movie.entity.ProjectionTechnology;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieScreeningVersionRepository
        extends JpaRepository<MovieScreeningVersion, UUID> {

    List<MovieScreeningVersion> findAllByMovie_IdOrderByCreatedAtAscIdAsc(UUID movieId);

    List<MovieScreeningVersion> findAllByMovie_IdAndActiveOrderByCreatedAtAscIdAsc(
            UUID movieId, boolean active);

    Optional<MovieScreeningVersion> findByIdAndMovie_Id(UUID id, UUID movieId);

    Optional<MovieScreeningVersion>
            findByMovie_IdAndContentFormatAndProjectionTechnologyAndAudioLanguageAndPresentationTypeAndSubtitleLanguage(
                    UUID movieId,
                    ContentFormat contentFormat,
                    ProjectionTechnology projectionTechnology,
                    String audioLanguage,
                    PresentationType presentationType,
                    String subtitleLanguage);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select screeningVersion
            from MovieScreeningVersion screeningVersion
            where screeningVersion.movie.id = :movieId
            order by screeningVersion.createdAt asc, screeningVersion.id asc
            """)
    List<MovieScreeningVersion> findAllForUpdate(@Param("movieId") UUID movieId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select screeningVersion
            from MovieScreeningVersion screeningVersion
            where screeningVersion.id = :versionId
              and screeningVersion.movie.id = :movieId
            """)
    Optional<MovieScreeningVersion> findForUpdate(
            @Param("movieId") UUID movieId, @Param("versionId") UUID versionId);
}
