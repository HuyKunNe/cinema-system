package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateCinemaRequest;
import com.cinema.inventory.dto.request.UpdateCinemaRequest;
import com.cinema.inventory.dto.response.CinemaResponse;
import com.cinema.inventory.service.CinemaService;

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
@RequestMapping("/api/v1/cinemas")
public class CinemaController {

    private final CinemaService cinemaService;

    public CinemaController(CinemaService cinemaService) {
        this.cinemaService = cinemaService;
    }

    @Operation(operationId = "createCinema")
    @PostMapping
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<CinemaResponse> createCinema(@Valid @RequestBody CreateCinemaRequest request) {

        CinemaResponse response = cinemaService.create(request);

        return ResponseEntity.created(URI.create("/api/v1/cinemas/" + response.id()))
                .body(response);
    }

    @GetMapping("/{cinemaId}")
    public ResponseEntity<CinemaResponse> getCinemaById(@PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(cinemaService.getById(cinemaId));
    }

    @GetMapping
    public ResponseEntity<List<CinemaResponse>> getActiveCinemas(
            @RequestParam(name = "city", required = false) String city) {

        return ResponseEntity.ok(cinemaService.getActiveCinemas(city));
    }

    @GetMapping("/management")
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<List<CinemaResponse>> getManagedCinemas(
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "active", required = false) Boolean active) {

        return ResponseEntity.ok(cinemaService.getManagedCinemas(city, active));
    }

    @PutMapping("/{cinemaId}")
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<CinemaResponse> updateCinema(
            @PathVariable("cinemaId") UUID cinemaId,
            @Valid @RequestBody UpdateCinemaRequest request) {

        return ResponseEntity.ok(cinemaService.update(cinemaId, request));
    }
}
