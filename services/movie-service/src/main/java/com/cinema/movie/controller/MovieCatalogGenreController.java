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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/movies/catalog")
@Tag(name = "movie-catalog-genre-controller")
public class MovieCatalogGenreController {

    private final MovieCatalogGenreService service;

    public MovieCatalogGenreController(MovieCatalogGenreService service) {
        this.service = service;
    }

    @Operation(
            operationId = "getMovieCatalogGenres",
            summary = "Get genres present in the matching movie catalog",
            description =
                    """
                    Public read API.

                    Returns distinct genres attached to movies matching
                    the optional status and candidate movie ID filters.

                    The result is independent of movie pagination,
                    movie sorting and the selected genre filter.

                    Movie IDs are candidate filters, not proof that a movie
                    has bookable showtimes at a cinema.
                    """)
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Matching genres, including an empty list",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        GenreResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid status, movie UUID or movie ID filter",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @GetMapping(
            value = "/genres",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GenreResponse>> findGenres(
            @RequestParam(value = "status", required = false) MovieStatus status,
            @Parameter(
                            description =
                                    "Optional candidate movie UUIDs. "
                                            + "Send repeated movieIds query parameters. "
                                            + "Uses the same configured ID limit as the movie catalog.")
                    @RequestParam(value = "movieIds", required = false)
                    List<UUID> movieIds) {

        return ResponseEntity.ok(service.findGenres(status, movieIds));
    }
}
