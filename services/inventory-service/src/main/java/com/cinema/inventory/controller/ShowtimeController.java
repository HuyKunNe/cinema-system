package com.cinema.inventory.controller;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.service.ShowtimeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    public ShowtimeController(
            ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> create(
            @Valid @RequestBody CreateShowtimeRequest request) {

        ShowtimeResponse response = showtimeService.create(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/showtimes/" + response.id()))
                .body(response);
    }

    @GetMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> getById(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(
                showtimeService.getById(showtimeId));
    }

    @GetMapping("/by-room/{roomId}")
    public ResponseEntity<List<ShowtimeResponse>> getByRoomId(
            @PathVariable("roomId") UUID roomId) {

        return ResponseEntity.ok(
                showtimeService.getByRoomId(roomId));
    }

    @GetMapping("/by-movie/{movieId}")
    public ResponseEntity<List<ShowtimeResponse>> getByMovieId(
            @PathVariable("movieId") UUID movieId) {

        return ResponseEntity.ok(
                showtimeService.getByMovieId(movieId));
    }

    @GetMapping
    public ResponseEntity<List<ShowtimeResponse>> getByTimeRange(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,

            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {

        return ResponseEntity.ok(
                showtimeService.getByTimeRange(from, to));
    }

    @PutMapping("/{showtimeId}")
    public ResponseEntity<ShowtimeResponse> update(
            @PathVariable("showtimeId") UUID showtimeId,
            @Valid @RequestBody UpdateShowtimeRequest request) {

        return ResponseEntity.ok(
                showtimeService.update(showtimeId, request));
    }

    @PatchMapping("/{showtimeId}/open")
    public ResponseEntity<ShowtimeResponse> openForBooking(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(
                showtimeService.openForBooking(showtimeId));
    }

    @PatchMapping("/{showtimeId}/close")
    public ResponseEntity<ShowtimeResponse> close(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(
                showtimeService.close(showtimeId));
    }

    @PatchMapping("/{showtimeId}/cancel")
    public ResponseEntity<ShowtimeResponse> cancel(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(
                showtimeService.cancel(showtimeId));
    }

    @PatchMapping("/{showtimeId}/complete")
    public ResponseEntity<ShowtimeResponse> complete(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok(
                showtimeService.complete(showtimeId));
    }
}