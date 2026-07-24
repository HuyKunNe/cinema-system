package com.cinema.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.inventory.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findAllByRoom_IdOrderBySeatNumberAsc(UUID roomId);

    List<Seat> findAllByRoom_IdAndActiveTrueOrderBySeatNumberAsc(UUID roomId);

    boolean existsByRoom_IdAndSeatNumberIgnoreCase(UUID roomId, String seatNumber);

    boolean existsByRoom_IdAndSeatNumberIgnoreCaseAndIdNot(UUID roomId, String seatNumber, UUID id);
}
