package com.cinema.inventory.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {

    List<Showtime> findAllByRoom_IdOrderByStartsAtAsc(UUID roomId);

    List<Showtime> findAllByMovieIdOrderByStartsAtAsc(UUID movieId);

    List<Showtime> findAllByStartsAtBetweenOrderByStartsAtAsc(
            OffsetDateTime from, OffsetDateTime to);

    @Query(
            """
            select case
                when count(showtime) > 0 then true
                else false
            end
            from Showtime showtime
            where showtime.room.id = :roomId
              and showtime.status not in :excludedStatuses
              and showtime.startsAt < :endsAt
              and showtime.endsAt > :startsAt
            """)
    boolean existsOverlappingShowtime(
            @Param("roomId") UUID roomId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("excludedStatuses") Collection<ShowtimeStatus> excludedStatuses);

    @Query(
            """
            select case
                when count(showtime) > 0 then true
                else false
            end
            from Showtime showtime
            where showtime.room.id = :roomId
              and showtime.id <> :showtimeId
              and showtime.status not in :excludedStatuses
              and showtime.startsAt < :endsAt
              and showtime.endsAt > :startsAt
            """)
    boolean existsOverlappingShowtimeExcludingId(
            @Param("roomId") UUID roomId,
            @Param("showtimeId") UUID showtimeId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("endsAt") OffsetDateTime endsAt,
            @Param("excludedStatuses") Collection<ShowtimeStatus> excludedStatuses);

    @Query(
            """
            select showtime
            from Showtime showtime
            join fetch showtime.room room
            join fetch room.cinema cinema
            where cinema.id = :cinemaId
              and cinema.active = true
              and room.active = true
              and showtime.status = :status
              and showtime.startsAt >= :from
              and showtime.startsAt < :to
              and showtime.startsAt > :now
              and (:movieId is null or showtime.movieId = :movieId)
            order by showtime.startsAt asc, showtime.id asc
            """)
    List<Showtime> findBookableShowtimes(
            @Param("cinemaId") UUID cinemaId,
            @Param("movieId") UUID movieId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("now") OffsetDateTime now,
            @Param("status") ShowtimeStatus status);

    @Query(
            """
            select case
                when count(showtime) > 0 then true
                else false
            end
            from Showtime showtime
            join showtime.room room
            join room.cinema cinema
            where showtime.id = :showtimeId
              and showtime.status = :status
              and showtime.startsAt > :now
              and room.active = true
              and cinema.active = true
            """)
    boolean existsHoldEligibleShowtime(
            @Param("showtimeId") UUID showtimeId,
            @Param("now") OffsetDateTime now,
            @Param("status") ShowtimeStatus status);

    @Query(
            """
            select showtime
            from Showtime showtime
            join fetch showtime.room room
            join fetch room.cinema
            left join fetch showtime.roomLayout
            where showtime.id = :showtimeId
            """)
    Optional<Showtime> findByIdForSeatMap(@Param("showtimeId") UUID showtimeId);
}
