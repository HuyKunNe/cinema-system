package com.cinema.inventory.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.enums.ShowtimeStatus;

import jakarta.persistence.LockModeType;

public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    List<Cinema> findAllByActiveTrueOrderByNameAsc();

    List<Cinema> findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(String city);

    List<Cinema> findAllByOrderByNameAscIdAsc();

    List<Cinema> findAllByActiveOrderByNameAscIdAsc(boolean active);

    List<Cinema> findAllByCityIgnoreCaseOrderByNameAscIdAsc(String city);

    List<Cinema> findAllByCityIgnoreCaseAndActiveOrderByNameAscIdAsc(String city, boolean active);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cinema from Cinema cinema where cinema.id = :cinemaId")
    Optional<Cinema> findByIdForUpdate(@Param("cinemaId") UUID cinemaId);

    @Query(
            """
            select case when count(showtime) > 0 then true else false end
            from Showtime showtime
            where showtime.room.cinema.id = :cinemaId
              and showtime.status = :status
              and showtime.startsAt >= :from
            """)
    boolean existsShowtimeWithStatusStartingAtOrAfter(
            @Param("cinemaId") UUID cinemaId,
            @Param("status") ShowtimeStatus status,
            @Param("from") OffsetDateTime from);
}
