package com.cinema.inventory.repository;

import com.cinema.inventory.entity.Room;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findAllByCinema_IdOrderByNameAsc(UUID cinemaId);

    List<Room> findAllByCinema_IdAndActiveTrueOrderByNameAsc(UUID cinemaId);

    boolean existsByCinema_IdAndNameIgnoreCase(UUID cinemaId, String name);

    boolean existsByCinema_IdAndNameIgnoreCaseAndIdNot(UUID cinemaId, String name, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") UUID roomId);
}
