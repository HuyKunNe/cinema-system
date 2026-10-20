package com.cinema.inventory.repository;

import com.cinema.inventory.entity.Seat;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    // Catalog queries retain the legacy seat-management contract.
    // Layout-managed seats are accessed through layout cells or explicit IDs.

    @Query(
            """
            select seat
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
            order by seat.seatNumber asc
            """)
    List<Seat> findAllByRoom_IdOrderBySeatNumberAsc(@Param("roomId") UUID roomId);

    @Query(
            """
            select seat
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
              and seat.active = true
            order by seat.seatNumber asc
            """)
    List<Seat> findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(@Param("roomId") UUID roomId);

    @Query(
            """
            select case when count(seat) > 0 then true else false end
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
              and lower(seat.seatNumber) = lower(:seatNumber)
            """)
    boolean existsByRoom_IdAndSeatNumberIgnoreCase(
            @Param("roomId") UUID roomId, @Param("seatNumber") String seatNumber);

    @Query(
            """
            select case when count(seat) > 0 then true else false end
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
              and lower(seat.seatNumber) = lower(:seatNumber)
              and seat.id <> :id
            """)
    boolean existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(
            @Param("roomId") UUID roomId,
            @Param("seatNumber") String seatNumber,
            @Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(
            """
            select seat
            from Seat seat
            where seat.room.id = :roomId
              and seat.id in :seatIds
            order by seat.id asc
            """)
    List<Seat> findAllForLayoutGeneration(
            @Param("roomId") UUID roomId, @Param("seatIds") Collection<UUID> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select seat
            from Seat seat
            where seat.id = :seatId
              and seat.room.id = :roomId
            """)
    Optional<Seat> findForActiveUpdate(@Param("seatId") UUID seatId, @Param("roomId") UUID roomId);

    @Query(
            """
            select seat
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
            order by seat.seatNumber asc, seat.id asc
            """)
    List<Seat> findAllByRoom_IdOrderBySeatNumberAscIdAsc(@Param("roomId") UUID roomId);

    @Query(
            """
            select seat
            from Seat seat
            where seat.room.id = :roomId
              and seat.layoutManaged = false
              and seat.active = :active
            order by seat.seatNumber asc, seat.id asc
            """)
    List<Seat> findAllByRoom_IdAndActiveOrderBySeatNumberAscIdAsc(
            @Param("roomId") UUID roomId, @Param("active") boolean active);
}
