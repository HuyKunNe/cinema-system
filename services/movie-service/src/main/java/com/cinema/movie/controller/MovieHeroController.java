package com.cinema.movie.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movie.dto.request.UpdateMovieHeroRequest;
import com.cinema.movie.dto.response.MovieHeroConfigurationResponse;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.service.MovieHeroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieHeroController {

    private final MovieHeroService movieHeroService;

    public MovieHeroController(MovieHeroService movieHeroService) {
        this.movieHeroService = movieHeroService;
    }

    @GetMapping("/hero")
    public ResponseEntity<List<MovieResponse>> getHeroMovies(
            @RequestParam(value = "limit", required = false)
            Integer limit,

            @RequestParam(value = "movieIds", required = false)
            List<UUID> movieIds) {

        return ResponseEntity.ok(movieHeroService.findHero(limit, movieIds));
    }

    @PatchMapping("/{movieId}/hero")
    public ResponseEntity<MovieHeroConfigurationResponse> updateMovieHero(
            @PathVariable("movieId") UUID movieId,
            @Valid @RequestBody UpdateMovieHeroRequest request) {

        return ResponseEntity.ok(movieHeroService.updateHero(movieId, request));
    }
}
