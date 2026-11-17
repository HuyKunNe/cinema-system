package com.cinema.movie.service;

import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.repository.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Service
public class MovieReleaseLifecycleService {
    private final MovieRepository repository;

    public MovieReleaseLifecycleService(MovieRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int promoteReleasedMovies(LocalDate today, OffsetDateTime updatedAt) {
        return repository.promoteReleasedMovies(
                MovieStatus.UPCOMING, MovieStatus.NOW_SHOWING, today, updatedAt);
    }
}
