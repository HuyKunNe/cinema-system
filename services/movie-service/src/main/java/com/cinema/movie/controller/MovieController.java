package com.cinema.movie.controller;

import com.cinema.common.response.model.PageResponse;
import com.cinema.movie.dto.request.CreateMovieRequest;
import com.cinema.movie.dto.request.MovieCatalogSort;
import com.cinema.movie.dto.request.UpdateMovieMetadataRequest;
import com.cinema.movie.dto.request.UpdateMovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.entity.MovieStatus;
import com.cinema.movie.service.MovieService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

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
    public ResponseEntity<MovieResponse> create(@Valid @RequestBody CreateMovieRequest request) {
        MovieResponse response = movieService.create(request);

        return ResponseEntity.created(URI.create("/api/v1/movies/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> findById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(movieService.findById(id));
    }

    @Operation(operationId = "getMovieCatalog", summary = "Get a paginated movie catalog")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A movie page, including empty pages"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid filter or pagination parameters",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "404",
                description = "A selected movie no longer exists",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @GetMapping("/catalog")
    public ResponseEntity<PageResponse<MovieResponse>> findCatalog(
            @RequestParam(value = "status", required = false) MovieStatus status,
            @RequestParam(value = "genre", required = false) UUID genreId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", required = false) Integer size,
            @Parameter(
                            description =
                                    "Optional candidate movie UUIDs. Send repeated movieIds query"
                                        + " parameters. Filtering is applied before pagination and"
                                        + " counting. The configured input limit defaults to 100.")
                    @RequestParam(value = "movieIds", required = false)
                    List<UUID> movieIds,
            @Parameter(
                            description =
                                    "Catalog order. Release-date sorts put null dates last. Title"
                                        + " order uses the database collation. All orders use"
                                        + " ascending movie ID as the final tie-breaker.")
                    @RequestParam(value = "sort", defaultValue = "RELEASE_DESC")
                    MovieCatalogSort sort) {

        return ResponseEntity.ok(
                movieService.findCatalog(status, genreId, page, size, movieIds, sort));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> findAll() {
        return ResponseEntity.ok(movieService.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> update(
            @PathVariable("id") UUID id, @Valid @RequestBody UpdateMovieRequest request) {
        return ResponseEntity.ok(movieService.update(id, request));
    }

    @Operation(
            operationId = "updateMovieMetadata",
            summary = "Replace movie metadata",
            description =
                    "Requires movie:manage. Both properties must be present. "
                            + "Null clears a value. Artwork fallback is handled by the frontend.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Updated movie",
                content = @Content(schema = @Schema(implementation = MovieResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request body or metadata",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "Required authority is missing",
                content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Movie not found",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PutMapping("/{id}/metadata")
    public ResponseEntity<MovieResponse> updateMetadata(
            @PathVariable("id") UUID id, @Valid @RequestBody UpdateMovieMetadataRequest request) {

        return ResponseEntity.ok(movieService.updateMetadata(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        movieService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
