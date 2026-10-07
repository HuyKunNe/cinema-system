package com.cinema.movie.repository;

import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.MovieStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, UUID id);

    @Override
    @EntityGraph(attributePaths = "genres")
    Optional<Movie> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "genres")
    List<Movie> findAll();

    @Query(
            value =
                    """
                    select movie.id
                    from Movie movie
                    where (:status is null or movie.status = :status)
                      and (
                        :genreId is null
                        or exists (
                            select genre.id
                            from Movie genreMovie
                            join genreMovie.genres genre
                            where genreMovie.id = movie.id
                              and genre.id = :genreId
                        )
                      )
                                        order by
                        case
                            when :catalogSort <> 'TITLE_ASC'
                                 and movie.releaseDate is null
                            then 1
                            else 0
                        end asc,
                        case
                            when :catalogSort = 'RELEASE_DESC'
                            then movie.releaseDate
                            else null
                        end desc,
                        case
                            when :catalogSort = 'RELEASE_ASC'
                            then movie.releaseDate
                            else null
                        end asc,
                        case
                            when :catalogSort = 'TITLE_ASC'
                            then movie.title
                            else null
                        end asc,
                        movie.id asc
                    """,
            countQuery =
                    """
                    select count(movie)
                    from Movie movie
                    where (:status is null or movie.status = :status)
                      and (
                        :genreId is null
                        or exists (
                            select genre.id
                            from Movie genreMovie
                            join genreMovie.genres genre
                            where genreMovie.id = movie.id
                              and genre.id = :genreId
                        )
                      )
                    """)
    Page<UUID> findCatalogMovieIdsSorted(
            @Param("status") MovieStatus status,
            @Param("genreId") UUID genreId,
            @Param("catalogSort") String catalogSort,
            Pageable pageable);

    default Page<UUID> findCatalogMovieIds(MovieStatus status, UUID genreId, Pageable pageable) {

        return findCatalogMovieIdsSorted(status, genreId, "RELEASE_DESC", pageable);
    }

    @Query(
            """
            select distinct movie
            from Movie movie
            left join fetch movie.genres
            where movie.id in :movieIds
            """)
    List<Movie> findAllWithGenresByIdIn(@Param("movieIds") Collection<UUID> movieIds);

    @Query(
            """
            select movie.id
            from Movie movie
            where movie.status = :status
              and movie.heroEnabled = true
              and (movie.heroStartsAt is null or movie.heroStartsAt <= :now)
              and (movie.heroEndsAt is null or movie.heroEndsAt > :now)
            order by movie.heroPriority asc, movie.id asc
            """)
    List<UUID> findHeroMovieIds(
            @Param("status") MovieStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    @Query(
            """
            select movie.id
            from Movie movie
            where movie.status = :status
              and movie.heroEnabled = true
              and (movie.heroStartsAt is null or movie.heroStartsAt <= :now)
              and (movie.heroEndsAt is null or movie.heroEndsAt > :now)
              and movie.id in :movieIds
            order by movie.heroPriority asc, movie.id asc
            """)
    List<UUID> findHeroMovieIdsByMovieIds(
            @Param("status") MovieStatus status,
            @Param("now") OffsetDateTime now,
            @Param("movieIds") Collection<UUID> movieIds,
            Pageable pageable);

    @Query(
            value =
                    """
                    select movie.id
                    from Movie movie
                    where movie.id in :movieIds
                      and (:status is null or movie.status = :status)
                      and (
                        :genreId is null
                        or exists (
                            select genre.id
                            from Movie genreMovie
                            join genreMovie.genres genre
                            where genreMovie.id = movie.id
                              and genre.id = :genreId
                        )
                      )
                    order by
                        case
                            when :catalogSort <> 'TITLE_ASC'
                                 and movie.releaseDate is null
                            then 1
                            else 0
                        end asc,
                        case
                            when :catalogSort = 'RELEASE_DESC'
                            then movie.releaseDate
                            else null
                        end desc,
                        case
                            when :catalogSort = 'RELEASE_ASC'
                            then movie.releaseDate
                            else null
                        end asc,
                        case
                            when :catalogSort = 'TITLE_ASC'
                            then movie.title
                            else null
                        end asc,
                        movie.id asc
                    """,
            countQuery =
                    """
                    select count(movie)
                    from Movie movie
                    where movie.id in :movieIds
                      and (:status is null or movie.status = :status)
                      and (
                        :genreId is null
                        or exists (
                            select genre.id
                            from Movie genreMovie
                            join genreMovie.genres genre
                            where genreMovie.id = movie.id
                              and genre.id = :genreId
                        )
                      )
                    """)
    Page<UUID> findCatalogMovieIdsByMovieIdsSorted(
            @Param("status") MovieStatus status,
            @Param("genreId") UUID genreId,
            @Param("movieIds") Collection<UUID> movieIds,
            @Param("catalogSort") String catalogSort,
            Pageable pageable);

    default Page<UUID> findCatalogMovieIdsByMovieIds(
            MovieStatus status, UUID genreId, Collection<UUID> movieIds, Pageable pageable) {

        return findCatalogMovieIdsByMovieIdsSorted(
                status, genreId, movieIds, "RELEASE_DESC", pageable);
    }
}
