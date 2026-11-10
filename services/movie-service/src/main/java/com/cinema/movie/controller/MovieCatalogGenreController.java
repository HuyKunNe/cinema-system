package com.cinema.movie.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movie.dto.response.GenreResponse;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.service.MovieCatalogGenreService;

@RestController
@RequestMapping("/api/v1/movies/catalog")
public class MovieCatalogGenreController {

    private final MovieCatalogGenreService service;

    public MovieCatalogGenreController(MovieCatalogGenreService service) {
        this.service = service;
    }

    @GetMapping(
            value = "/genres",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GenreResponse>> getMovieCatalogGenres(
            @RequestParam(value = "status", required = false) MovieStatus status,
                    @RequestParam(value = "movieIds", required = false)
                    List<UUID> movieIds) {

        return ResponseEntity.ok(service.findGenres(status, movieIds));
    }
}
