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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieHeroController {

    private final MovieHeroService movieHeroService;

    public MovieHeroController(MovieHeroService movieHeroService) {
        this.movieHeroService = movieHeroService;
    }

    @Operation(
            operationId = "getHeroMovies",
            summary = "Get editorially selected movies for the hero",
            description =
                    "Returns eligible NOW_SHOWING movies in editorial order. "
                            + "Default limit is configured, initially 4; maximum initially 10. "
                            + "movieIds is an optional candidate filter applied before limiting. "
                            + "An empty supplied filter returns an empty list.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Ordered movie list; may be empty",
                content = @Content(
                        array = @ArraySchema(
                                schema = @Schema(implementation = MovieResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "A selected movie disappeared during loading",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class)))
    })
    @GetMapping("/hero")
    public ResponseEntity<List<MovieResponse>> findHero(
            @Parameter(description = "Configured default when omitted; supported range 1–10.")
            @RequestParam(value = "limit", required = false)
            Integer limit,

            @Parameter(
                    description =
                            "Optional UUID candidate list. Configured maximum initially 100. "
                                    + "Use repeated movieIds query parameters.",
                    array = @ArraySchema(
                            schema = @Schema(type = "string", format = "uuid"),
                            maxItems = 100))
            @RequestParam(value = "movieIds", required = false)
            List<UUID> movieIds) {

        return ResponseEntity.ok(movieHeroService.findHero(limit, movieIds));
    }

    @Operation(
            operationId = "updateMovieHero",
            summary = "Update movie hero configuration",
            description =
                    "Requires movie:manage. Omitted properties are preserved. "
                            + "Null removes a time bound; enabled and priority cannot be null.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Resulting hero configuration",
                content = @Content(
                        schema = @Schema(
                                implementation = MovieHeroConfigurationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid body or resulting configuration",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Movie not found",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Missing movie:manage authority",
                content = @Content)
    })
    @PatchMapping("/{movieId}/hero")
    public ResponseEntity<MovieHeroConfigurationResponse> updateHero(
            @PathVariable("movieId") UUID movieId,
            @Valid @RequestBody UpdateMovieHeroRequest request) {

        return ResponseEntity.ok(movieHeroService.updateHero(movieId, request));
    }
}
