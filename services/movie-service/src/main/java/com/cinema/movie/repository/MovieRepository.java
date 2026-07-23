package com.cinema.movie.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cinema.movie.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    boolean existsByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCaseAndIdNot(
            String title,
            UUID id);

    @Override
    @EntityGraph(attributePaths = "genres")
    Optional<Movie> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "genres")
    List<Movie> findAll();
}
