package com.cinema.inventory.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cinema.inventory.entity.Seat;

import jakarta.persistence.LockModeType;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findAllByRoom_IdOrderBySeatNumberAsc(UUID roomId);

    List<Seat> findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(UUID roomId);

    boolean existsByRoom_IdAndSeatNumberIgnoreCase(UUID roomId, String seatNumber);

    boolean existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(UUID roomId, String seatNumber, UUID id);

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
}
