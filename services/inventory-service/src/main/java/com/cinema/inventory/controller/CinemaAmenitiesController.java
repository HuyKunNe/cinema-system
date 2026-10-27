package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaAmenitiesRequest;
import com.cinema.inventory.dto.response.CinemaAmenitiesResponse;
import com.cinema.inventory.service.CinemaAmenitiesService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/amenities")
public class CinemaAmenitiesController {

    private final CinemaAmenitiesService service;

    public CinemaAmenitiesController(
            CinemaAmenitiesService service) {

        this.service = service;
    }

    @GetMapping
    @SecurityRequirements
    public ResponseEntity<CinemaAmenitiesResponse> getCinemaAmenities(
            @PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(service.get(cinemaId));
    }

    @PutMapping
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<CinemaAmenitiesResponse> replaceCinemaAmenities(
            @PathVariable("cinemaId") UUID cinemaId,
            @Valid @RequestBody ReplaceCinemaAmenitiesRequest request) {

        return ResponseEntity.ok(
                service.replace(cinemaId, request));
    }
}
