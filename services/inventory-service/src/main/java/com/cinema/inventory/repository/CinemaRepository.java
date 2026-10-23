package com.cinema.inventory.repository;

import com.cinema.inventory.entity.Cinema;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    List<Cinema> findAllByActiveTrueOrderByNameAsc();

    List<Cinema> findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(String city);

    List<Cinema> findAllByOrderByNameAscIdAsc();

    List<Cinema> findAllByActiveOrderByNameAscIdAsc(boolean active);

    List<Cinema> findAllByCityIgnoreCaseOrderByNameAscIdAsc(String city);

    List<Cinema> findAllByCityIgnoreCaseAndActiveOrderByNameAscIdAsc(String city, boolean active);
}
