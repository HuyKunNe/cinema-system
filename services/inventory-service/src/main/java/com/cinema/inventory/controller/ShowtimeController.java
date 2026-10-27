package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateShowtimeBatchRequest;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.service.ShowtimeService;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @ApiResponse(
            responseCode = "409",
            description =
                    "Layout selection required, selected layout is not published, "
                            + "room or cinema inactive, schedule overlap "
                            + "or seat-generation conflict",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            implementation =
                                                    com.cinema.common.response.model.ApiResponse
                                                            .class)))
    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(
            @Valid @RequestBody CreateShowtimeRequest request) {

        ShowtimeResponse response = showtimeService.create(request);

        return ResponseEntity.created(URI.create("/api/v1/showtimes/" + response.id()))
                .body(response);
    }

    @SecurityRequirement(
            name = "oauth2",
            scopes = {"showtime:manage"})
    @PostMapping("/batch")
    public ResponseEntity<List<ShowtimeResponse>> createShowtimeBatch(
            @Valid @RequestBody CreateShowtimeBatchRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(showtimeService.createBatch(request));
    }

    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Matching showtimes, or an empty array",
                content =
                        @Content(
                                array =
                                        @ArraySchema(
                                                schema =
                                                        @Schema(
                                                                implementation =
                                                                        ShowtimeResponse.class)))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid query parameters or time range",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @GetMapping("/bookable")
    public ResponseEntity<List<ShowtimeResponse>> getBookableShowtimes(
            @RequestParam("cinemaId") UUID cinemaId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to,
            @RequestParam(value = "movieId", required = false) UUID movieId) {

        return ResponseEntity.ok(showtimeService.getBookableShowtimes(cinemaId, movieId, from, to));
    }

    @GetMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> getShowtimeById(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(showtimeService.getById(showtimeId));
    }

    @GetMapping("/by-room/{roomId}")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByRoom(
            @PathVariable("roomId") UUID roomId) {

        return ResponseEntity.ok(showtimeService.getByRoomId(roomId));
    }

    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByMovie(
            @PathVariable("movieId") UUID movieId) {

        return ResponseEntity.ok(showtimeService.getByMovieId(movieId));
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getShowtimesByTimeRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to) {

        return ResponseEntity.ok(showtimeService.getByTimeRange(from, to));
    }

    @PutMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> updateShowtime(
            @PathVariable("showtimeId") UUID showtimeId,
            @Valid @RequestBody UpdateShowtimeRequest request) {

        return ResponseEntity.ok(showtimeService.update(showtimeId, request));
    }

    @PatchMapping("/{showtimeId}/open")
    public ResponseEntity<ShowtimeResponse> openShowtimeForBooking(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(showtimeService.openForBooking(showtimeId));
    }

    @PatchMapping("/{showtimeId}/close")
    public ResponseEntity<ShowtimeResponse> closeShowtime(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(showtimeService.close(showtimeId));
    }

    @PatchMapping("/{showtimeId}/cancel")
    public ResponseEntity<ShowtimeResponse> cancelShowtime(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(showtimeService.cancel(showtimeId));
    }

    @PatchMapping("/{showtimeId}/complete")
    public ResponseEntity<ShowtimeResponse> completeShowtime(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(showtimeService.complete(showtimeId));
    }
}
