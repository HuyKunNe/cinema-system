package com.cinema.movie.controller;

import com.cinema.movie.dto.request.CreateMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.UpdateMovieScreeningVersionActiveRequest;
import com.cinema.movie.dto.response.MovieScreeningVersionResponse;
import com.cinema.movie.service.MovieScreeningVersionService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/movies/{movieId}/screening-versions")
public class MovieScreeningVersionController {

    private final MovieScreeningVersionService service;

    public MovieScreeningVersionController(
            MovieScreeningVersionService service) {

        this.service = service;
    }

    @GetMapping
    @SecurityRequirements
    public ResponseEntity<List<MovieScreeningVersionResponse>> listMovieScreeningVersions(
            @PathVariable("movieId") UUID movieId,
            @RequestParam(name = "active", required = false)
            Boolean active) {

        return ResponseEntity.ok(service.list(movieId, active));
    }

    @GetMapping("/{versionId}")
    @SecurityRequirements
    public ResponseEntity<MovieScreeningVersionResponse> getMovieScreeningVersion(
            @PathVariable("movieId") UUID movieId,
            @PathVariable("versionId") UUID versionId) {

        return ResponseEntity.ok(service.get(movieId, versionId));
    }

    @PostMapping
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"movie:manage"})
    public ResponseEntity<MovieScreeningVersionResponse> createMovieScreeningVersion(
            @PathVariable("movieId") UUID movieId,
            @Valid @RequestBody CreateMovieScreeningVersionRequest request) {

        var result = service.create(movieId, request);
        var response = result.response();

        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(URI.create(
                        "/api/v1/movies/"
                                + movieId
                                + "/screening-versions/"
                                + response.id()))
                .body(response);
    }

    @PutMapping("/{versionId}/active")
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"movie:manage"})
    public ResponseEntity<MovieScreeningVersionResponse> updateMovieScreeningVersionActive(
            @PathVariable("movieId") UUID movieId,
            @PathVariable("versionId") UUID versionId,
            @Valid
            @RequestBody UpdateMovieScreeningVersionActiveRequest request) {

        return ResponseEntity.ok(
                service.updateActive(movieId, versionId, request));
    }
}
