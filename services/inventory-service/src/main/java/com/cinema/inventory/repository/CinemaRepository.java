package com.cinema.inventory.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.inventory.entity.Cinema;

public interface CinemaRepository extends JpaRepository<Cinema, UUID> {

    List<Cinema> findAllByActiveTrueOrderByNameAsc();

    List<Cinema> findAllByCityIgnoreCaseAndActiveTrueOrderByNameAsc(String city);
}
