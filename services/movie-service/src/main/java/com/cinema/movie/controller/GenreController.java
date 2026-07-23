package com.cinema.movie.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.movie.dto.request.CreateGenreRequest;
import com.cinema.movie.dto.request.UpdateGenreRequest;
import com.cinema.movie.dto.response.GenreResponse;
import com.cinema.movie.service.GenreService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final GenreService genreService;

    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    @PostMapping
    public ResponseEntity<GenreResponse> create(
            @Valid @RequestBody CreateGenreRequest request) {
        GenreResponse response = genreService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/genres/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> findById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(genreService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<GenreResponse>> findAll() {
        return ResponseEntity.ok(genreService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGenreRequest request) {
        return ResponseEntity.ok(
                genreService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id) {
        genreService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
