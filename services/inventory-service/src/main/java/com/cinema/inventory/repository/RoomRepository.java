package com.cinema.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.inventory.entity.Room;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findAllByCinema_IdOrderByNameAsc(UUID cinemaId);

    List<Room> findAllByCinema_IdAndActiveTrueOrderByNameAsc(UUID cinemaId);

    boolean existsByCinema_IdAndNameIgnoreCase(UUID cinemaId, String name);

    boolean existsByCinema_IdAndNameIgnoreCaseAndIdNot(UUID cinemaId, String name, UUID id);
}
