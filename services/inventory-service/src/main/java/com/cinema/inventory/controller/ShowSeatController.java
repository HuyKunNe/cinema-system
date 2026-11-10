package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.service.ShowSeatService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/show-seats")
public class ShowSeatController {

    private final ShowSeatService showSeatService;

    public ShowSeatController(ShowSeatService showSeatService) {
        this.showSeatService = showSeatService;
    }

    @GetMapping("/{showSeatId}")
    public ResponseEntity<ShowSeatResponse> getShowSeatById(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.getById(showSeatId));
    }

    @GetMapping
    public ResponseEntity<List<ShowSeatResponse>> getShowSeatsByShowtime(
            @RequestParam("showtimeId") UUID showtimeId,
            @RequestParam(name = "availableOnly", defaultValue = "false") boolean availableOnly) {

        if (availableOnly) {
            return ResponseEntity.ok(showSeatService.getAvailableByShowtimeId(showtimeId));
        }

        return ResponseEntity.ok(showSeatService.getByShowtimeId(showtimeId));
    }

    @PutMapping("/{showSeatId}/hold")
    public ResponseEntity<ShowSeatResponse> holdShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody HoldShowSeatRequest request) {

        return ResponseEntity.ok(showSeatService.hold(showSeatId, request));
    }

    @PutMapping("/{showSeatId}/book")
    public ResponseEntity<ShowSeatResponse> bookShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody ShowSeatBookingRequest request) {

        return ResponseEntity.ok(showSeatService.book(showSeatId, request));
    }

    @PutMapping("/{showSeatId}/release")
    public ResponseEntity<ShowSeatResponse> releaseShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody ShowSeatBookingRequest request) {

        return ResponseEntity.ok(showSeatService.release(showSeatId, request));
    }

    @PutMapping("/{showSeatId}/unavailable")
    public ResponseEntity<ShowSeatResponse> makeUnavailable(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.makeUnavailable(showSeatId));
    }

    @PutMapping("/{showSeatId}/available")
    public ResponseEntity<ShowSeatResponse> makeAvailable(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.makeAvailable(showSeatId));
    }
}
