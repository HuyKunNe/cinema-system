package com.cinema.movie.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.movie.entity.Genre;

public interface GenreRepository extends JpaRepository<Genre, UUID> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
