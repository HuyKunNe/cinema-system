package com.cinema.inventory.repository;

import com.cinema.inventory.entity.Cinema;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select cinema from Cinema cinema where cinema.id = :cinemaId")
    Optional<Cinema> findByIdForRead(@Param("cinemaId") UUID cinemaId);
}
