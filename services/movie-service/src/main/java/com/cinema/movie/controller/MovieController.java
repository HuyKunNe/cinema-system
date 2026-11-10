package com.cinema.movie.controller;

import com.cinema.common.response.model.PageResponse;
import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.MovieCatalogSort;
import com.cinema.movie.dto.request.UpdateMovieMetadataRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.service.MovieService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(
            @Valid @RequestBody CreateMovieRequest request) {

        MovieResponse response = movieService.create(request);

        return ResponseEntity.created(URI.create("/api/v1/movies/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> findMovieById(
            @PathVariable("id") UUID id) {

        return ResponseEntity.ok(movieService.findById(id));
    }

    @GetMapping("/catalog")
    public ResponseEntity<PageResponse<MovieResponse>> getMovieCatalog(
            @RequestParam(value = "status", required = false) MovieStatus status,
            @RequestParam(value = "genre", required = false) UUID genreId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size,
                    @RequestParam(value = "movieIds", required = false)
                    List<UUID> movieIds,
                    @RequestParam(value = "sort", defaultValue = "RELEASE_DESC")
                    MovieCatalogSort sort) {

        return ResponseEntity.ok(
                movieService.findCatalog(status, genreId, page, size, movieIds, sort));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> findAllMovies() {
        return ResponseEntity.ok(movieService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateMovieRequest request) {

        return ResponseEntity.ok(movieService.update(id, request));
    }

    @PutMapping("/{id}/metadata")
    public ResponseEntity<MovieResponse> updateMovieMetadata(
            @PathVariable("id") UUID id, @Valid @RequestBody UpdateMovieMetadataRequest request) {

        return ResponseEntity.ok(movieService.updateMetadata(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable("id") UUID id) {
        movieService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
