package com.cinema.inventory.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.service.CinemaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @PostMapping
    public ResponseEntity<CinemaResponse> create(@Valid @RequestBody CreateCinemaRequest request) {

        CinemaResponse response = cinemaService.create(request);

        return ResponseEntity.created(URI.create("/api/v1/cinemas/" + response.id()))
                .body(response);
    }

    @GetMapping("/{cinemaId}")
    public ResponseEntity<CinemaResponse> getById(@PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(cinemaService.getById(cinemaId));
    }

    @GetMapping
    public ResponseEntity<List<CinemaResponse>> getActiveCinemas(
            @RequestParam(name = "city", required = false) String city) {

        return ResponseEntity.ok(cinemaService.getActiveCinemas(city));
    }

    @Operation(
            operationId = "getManagedCinemas",
            summary = "List cinemas for inventory management",
            description = "Omit active to include both active and inactive cinemas.",
            security = {
                @SecurityRequirement(
                        name = "oauth2",
                        scopes = {"inventory:manage"})
            })
    @GetMapping("/management")
    public ResponseEntity<List<CinemaResponse>> getManagedCinemas(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "active", required = false) Boolean active) {

        return ResponseEntity.ok(cinemaService.getManagedCinemas(city, active));
    }

    @PutMapping("/{cinemaId}")
    public ResponseEntity<CinemaResponse> update(
            @PathVariable("cinemaId") UUID cinemaId,
            @Valid @RequestBody UpdateCinemaRequest request) {

        return ResponseEntity.ok(cinemaService.update(cinemaId, request));
    }
}
