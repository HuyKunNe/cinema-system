package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceShowtimePricingRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.service.ShowtimePricingService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"showtime:manage"})
public class ShowtimePricingController {

    private final ShowtimePricingService service;

    public ShowtimePricingController(ShowtimePricingService service) {

        this.service = service;
    }

    @PutMapping("/{showtimeId}/pricing")
    public ResponseEntity<ShowtimeResponse> replaceShowtimePricing(
            @PathVariable("showtimeId") UUID showtimeId,
            @Valid @RequestBody ReplaceShowtimePricingRequest request) {

        return ResponseEntity.ok(service.replace(showtimeId, request));
    }
}
