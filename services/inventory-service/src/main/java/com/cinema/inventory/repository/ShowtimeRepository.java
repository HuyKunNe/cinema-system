package com.cinema.inventory.repository;

import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository extends JpaRepository<Showtime, UUID> {

    @Query(
            value = """
                    select showtime
                    from Showtime showtime
                    join fetch showtime.room room
                    join fetch room.cinema cinema
                    left join fetch showtime.roomLayout
                    left join fetch showtime.pricingPolicyVersion pricing
                    left join fetch pricing.policy
                    where showtime.startsAt >= :from
                      and showtime.startsAt < :to
                      and (:cinemaId is null or cinema.id = :cinemaId)
                      and (:movieId is null or showtime.movieId = :movieId)
                      and (:roomId is null or room.id = :roomId)
                      and (:status is null or showtime.status = :status)
                    order by showtime.startsAt asc, showtime.id asc
                    """,
            countQuery = """
                    select count(showtime)
                    from Showtime showtime
                    join showtime.room room
                    join room.cinema cinema
                    where showtime.startsAt >= :from
                      and showtime.startsAt < :to
                      and (:cinemaId is null or cinema.id = :cinemaId)
                      and (:movieId is null or showtime.movieId = :movieId)
                      and (:roomId is null or room.id = :roomId)
                      and (:status is null or showtime.status = :status)
                    """)
    Page<Showtime> findManagedShowtimes(
            @Param("cinemaId") UUID cinemaId,
            @Param("movieId") UUID movieId,
            @Param("roomId") UUID roomId,
            @Param("status") ShowtimeStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable);

    boolean existsByRoomLayout_Id(UUID layoutId);

    boolean existsByPricingPolicyVersion_Id(UUID versionId);

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
              and showtime.endsAt > :salesWindowCutoff
              and (:movieId is null or showtime.movieId = :movieId)
            order by showtime.startsAt asc, showtime.id asc
            """)
    List<Showtime> findBookableShowtimes(
            @Param("cinemaId") UUID cinemaId,
            @Param("movieId") UUID movieId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("salesWindowCutoff") OffsetDateTime salesWindowCutoff,
            @Param("status") ShowtimeStatus status);

    @Query(
            """
            select showtime
            from Showtime showtime
            join fetch showtime.room room
            join fetch room.cinema cinema
            where showtime.status = :status
              and showtime.startsAt <= :latestStartsAt
              and showtime.endsAt > :salesWindowCutoff
            order by showtime.startsAt asc, showtime.id asc
            """)
    List<Showtime> findScheduledForAutomaticOpening(
            @Param("status") ShowtimeStatus status,
            @Param("latestStartsAt") OffsetDateTime latestStartsAt,
            @Param("salesWindowCutoff") OffsetDateTime salesWindowCutoff);

    List<UUID> findIdsByStatusInAndEndsAtLessThanEqualOrderByEndsAtAscIdAsc(
            Collection<ShowtimeStatus> statuses, OffsetDateTime endsAt);

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
              and showtime.endsAt > :salesWindowCutoff
              and room.active = true
              and cinema.active = true
            """)
    boolean existsHoldEligibleShowtime(
            @Param("showtimeId") UUID showtimeId,
            @Param("salesWindowCutoff") OffsetDateTime salesWindowCutoff,
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

    @Query(
            """
            select showtime.room.id
            from Showtime showtime
            where showtime.id = :showtimeId
            """)
    Optional<UUID> findRoomIdForScheduleChange(@Param("showtimeId") UUID showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select showtime
            from Showtime showtime
            where showtime.id = :showtimeId
            """)
    Optional<Showtime> findByIdForUpdate(@Param("showtimeId") UUID showtimeId);

    @Query(
            """
            select case when count(showtime) > 0 then true else false end
            from Showtime showtime
            where showtime.room.id = :roomId
              and showtime.status not in :excludedStatuses
              and (:excludedShowtimeId is null or showtime.id <> :excludedShowtimeId)
              and showtime.startsAt < :readyAt
              and coalesce(showtime.scheduling.readyAt, showtime.endsAt) > :startsAt
            """)
    boolean existsSchedulingConflict(
            @Param("roomId") UUID roomId,
            @Param("excludedShowtimeId") UUID excludedShowtimeId,
            @Param("startsAt") OffsetDateTime startsAt,
            @Param("readyAt") OffsetDateTime readyAt,
            @Param("excludedStatuses") Collection<ShowtimeStatus> excludedStatuses);

    @Query(
            """
            select showtime.movieId
            from Showtime showtime
            where showtime.id = :showtimeId
            """)
    Optional<UUID> findMovieIdForScheduleChange(@Param("showtimeId") UUID showtimeId);
}
