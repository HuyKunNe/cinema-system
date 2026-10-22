package com.cinema.movie.controller;

import com.cinema.common.api.screening.ReplaceScreeningCapabilitiesRequest;
import com.cinema.movie.dto.response.MovieScreeningCapabilitiesResponse;
import com.cinema.movie.service.MovieScreeningCapabilitiesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies/{movieId}/screening-capabilities")
@Tag(name = "movie-screening-capabilities-controller")
public class MovieScreeningCapabilitiesController {

    private final MovieScreeningCapabilitiesService service;

    public MovieScreeningCapabilitiesController(MovieScreeningCapabilitiesService service) {

        this.service = service;
    }

    @Operation(
            operationId = "getMovieScreeningCapabilities",
            summary = "Get declared movie screening formats and release date",
            description =
                    """
                    configuredFormats contains the screening combinations
                    declared as available for this movie release.

                    supportedFormatOptions is the server's format vocabulary.
                    It does not enable those formats for this movie.

                    releaseDate is the stored official release date and may be null.
                    This endpoint does not simulate a date or calculate prices.
                    """)
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<MovieScreeningCapabilitiesResponse> get(
            @PathVariable("movieId") UUID movieId) {

        return ResponseEntity.ok(service.get(movieId));
    }

    @Operation(
            operationId = "replaceMovieScreeningCapabilities",
            summary = "Replace all declared movie screening formats",
            description =
                    """
                    Requires movie:manage.
                    expectedVersion refers to movie.version.
                    Replaces the complete configured format set.
                    An empty set clears all declared screening combinations.
                    Does not change releaseDate or existing showtime snapshots.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"movie:manage"})
    @PutMapping
    public ResponseEntity<MovieScreeningCapabilitiesResponse> replace(
            @PathVariable("movieId") UUID movieId,
            @Valid @RequestBody ReplaceScreeningCapabilitiesRequest request) {

        return ResponseEntity.ok(service.replace(movieId, request));
    }
}
