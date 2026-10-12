package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateSeatRangeRequest;
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.service.SeatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

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
@RequestMapping("/api/v1/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public ResponseEntity<SeatResponse> create(
            @RequestParam("roomId") UUID roomId, @Valid @RequestBody CreateSeatRequest request) {

        SeatResponse response = seatService.create(roomId, request);

        return ResponseEntity.created(URI.create("/api/v1/seats/" + response.id())).body(response);
    }

    @PostMapping("/range")
    public ResponseEntity<List<SeatResponse>> createRange(
            @RequestParam("roomId") UUID roomId,
            @Valid @RequestBody CreateSeatRangeRequest request) {

        List<SeatResponse> response = seatService.createRange(roomId, request);

        return ResponseEntity.created(URI.create("/api/v1/seats?roomId=" + roomId)).body(response);
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> getById(@PathVariable("seatId") UUID seatId) {

        return ResponseEntity.ok(seatService.getById(seatId));
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getActiveSeats(@RequestParam("roomId") UUID roomId) {

        return ResponseEntity.ok(seatService.getActiveSeats(roomId));
    }

    @Operation(
            operationId = "getManagedSeats",
            summary = "List seats for inventory management",
            description = "Omit active to include both active and inactive seats.",
            security = {
                @SecurityRequirement(
                        name = "oauth2",
                        scopes = {"inventory:manage"})
            })
    @GetMapping("/management")
    public ResponseEntity<List<SeatResponse>> getManagedSeats(
            @RequestParam("roomId") UUID roomId,
            @RequestParam(name = "active", required = false) Boolean active) {

        return ResponseEntity.ok(seatService.getManagedSeats(roomId, active));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<SeatResponse> update(
            @PathVariable("seatId") UUID seatId, @Valid @RequestBody UpdateSeatRequest request) {

        return ResponseEntity.ok(seatService.update(seatId, request));
    }
}
