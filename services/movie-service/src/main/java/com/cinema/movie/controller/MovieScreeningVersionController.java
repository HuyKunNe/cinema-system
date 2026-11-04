package com.cinema.movie.controller;

import com.cinema.movie.dto.request.CreateMovieScreeningVersionRequest;
import com.cinema.movie.dto.request.UpdateMovieScreeningVersionActiveRequest;
import com.cinema.movie.dto.response.MovieScreeningVersionResponse;
import com.cinema.movie.service.MovieScreeningVersionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema =
                                    @Schema(
                                            implementation =
                                                    com.cinema.common.response.model.ApiResponse
                                                            .class))),
    @ApiResponse(
            responseCode = "404",
            description = "Movie or screening version not found",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema =
                                    @Schema(
                                            implementation =
                                                    com.cinema.common.response.model.ApiResponse
                                                            .class)))
})
public class MovieScreeningVersionController {

    private final MovieScreeningVersionService service;

    public MovieScreeningVersionController(MovieScreeningVersionService service) {

        this.service = service;
    }

    @Operation(
            operationId = "listMovieScreeningVersions",
            summary = "List screening versions belonging to a movie")
    @ApiResponse(
            responseCode = "200",
            description = "Screening versions, including an empty array",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    MovieScreeningVersionResponse
                                                                            .class))))
    @GetMapping
    @SecurityRequirements
    public ResponseEntity<List<MovieScreeningVersionResponse>> listMovieScreeningVersions(
            @PathVariable("movieId") UUID movieId,
            @Parameter(description = "Optional active-state filter; omit to return all versions.")
                    @RequestParam(name = "active", required = false)
                    Boolean active) {

        return ResponseEntity.ok(service.list(movieId, active));
    }

    @Operation(
            operationId = "getMovieScreeningVersion",
            summary = "Get a screening version belonging to a movie")
    @ApiResponse(
            responseCode = "200",
            description = "Screening version",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MovieScreeningVersionResponse.class)))
    @GetMapping("/{versionId}")
    @SecurityRequirements
    public ResponseEntity<MovieScreeningVersionResponse> getMovieScreeningVersion(
            @PathVariable("movieId") UUID movieId, @PathVariable("versionId") UUID versionId) {

        return ResponseEntity.ok(service.get(movieId, versionId));
    }

    @Operation(
            operationId = "createMovieScreeningVersion",
            summary = "Create a screening version",
            description =
                    "Creates an immutable definition. An identical movie, image format, "
                            + "projection technology, audio language, presentation type and "
                            + "subtitle language combination is rejected.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "New active screening version created",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        MovieScreeningVersionResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "MOVIE_SCREENING_VERSION_ALREADY_EXISTS",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "401",
                description = "SECURITY_AUTHENTICATION_REQUIRED",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "403",
                description = "SECURITY_ACCESS_DENIED: movie:manage permission is required",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PostMapping
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"movie:manage"})
    public ResponseEntity<MovieScreeningVersionResponse> createMovieScreeningVersion(
            @PathVariable("movieId") UUID movieId,
            @Valid @RequestBody CreateMovieScreeningVersionRequest request) {

        var result = service.create(movieId, request);
        var response = result.response();

        URI location =
                URI.create("/api/v1/movies/" + movieId + "/screening-versions/" + response.id());

        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(location)
                .body(response);
    }

    @Operation(
            operationId = "updateMovieScreeningVersionActive",
            summary = "Activate or deactivate a screening version")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Screening version with its current revision",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        MovieScreeningVersionResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "MOVIE_SCREENING_VERSION_CONFLICT: reload the version before updating",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "401",
                description = "SECURITY_AUTHENTICATION_REQUIRED",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "403",
                description = "SECURITY_ACCESS_DENIED: movie:manage permission is required",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PutMapping("/{versionId}/active")
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"movie:manage"})
    public ResponseEntity<MovieScreeningVersionResponse> updateMovieScreeningVersionActive(
            @PathVariable("movieId") UUID movieId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody UpdateMovieScreeningVersionActiveRequest request) {

        return ResponseEntity.ok(service.updateActive(movieId, versionId, request));
    }
}
