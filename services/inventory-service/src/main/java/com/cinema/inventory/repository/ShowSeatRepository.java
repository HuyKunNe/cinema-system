package com.cinema.inventory.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.enums.ShowSeatStatus;

import jakarta.persistence.LockModeType;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    boolean existsByShowtime_Id(UUID showtimeId);

    List<ShowSeat> findAllByShowtime_IdOrderBySeatNumberAsc(UUID showtimeId);

    List<ShowSeat> findAllByShowtime_IdAndStatusOrderBySeatNumberAsc(
            UUID showtimeId, ShowSeatStatus status);

    Optional<ShowSeat> findByShowtime_IdAndSeatNumberIgnoreCase(UUID showtimeId, String seatNumber);

    boolean existsByShowtime_IdAndSeat_Id(UUID showtimeId, UUID seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select showSeat
            from ShowSeat showSeat
            where showSeat.showtime.id = :showtimeId
              and showSeat.seatNumber in :seatNumbers
            order by showSeat.seatNumber asc
            """)
    List<ShowSeat> findAllByShowtimeIdAndSeatNumbersForUpdate(
            @Param("showtimeId") UUID showtimeId,
            @Param("seatNumbers") Collection<String> seatNumbers);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select showSeat
            from ShowSeat showSeat
            where showSeat.id = :showSeatId
            """)
    Optional<ShowSeat> findByIdForUpdate(@Param("showSeatId") UUID showSeatId);

    @Query(
            """
            select showSeat
            from ShowSeat showSeat
            where showSeat.status = :status
              and showSeat.holdExpiresAt <= :now
            order by showSeat.holdExpiresAt asc
            """)
    List<ShowSeat> findExpiredHolds(
            @Param("status") ShowSeatStatus status, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select showSeat
            from ShowSeat showSeat
            where showSeat.showtime.id = :showtimeId
              and showSeat.id in :showSeatIds
            order by showSeat.id asc
            """)
    List<ShowSeat> findAllByShowtimeIdAndIdsForUpdate(
            @Param("showtimeId") UUID showtimeId,
            @Param("showSeatIds") Collection<UUID> showSeatIds);

    @Query(
            """
            select showSeat.id
            from ShowSeat showSeat
            where showSeat.showtime.id = :showtimeId
              and showSeat.heldByBookingId = :bookingId
            order by showSeat.id asc
            """)
    List<UUID> findIdsByShowtimeIdAndBookingId(
            @Param("showtimeId") UUID showtimeId, @Param("bookingId") UUID bookingId);

    @Query(
            """
            select showSeat
            from ShowSeat showSeat
            join fetch showSeat.seat seat
            join fetch seat.room
            where showSeat.showtime.id = :showtimeId
            order by showSeat.seatNumber asc
            """)
    List<ShowSeat> findAllForSeatMapByShowtimeId(@Param("showtimeId") UUID showtimeId);
}
