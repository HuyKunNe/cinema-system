package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.CinemaRoomCapacitiesResponse;
import com.cinema.inventory.service.CinemaRoomCapacitiesService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/room-capacities")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaRoomCapacitiesController {

    private final CinemaRoomCapacitiesService service;

    public CinemaRoomCapacitiesController(CinemaRoomCapacitiesService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<CinemaRoomCapacitiesResponse> get(
            @PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(service.get(cinemaId));
    }
}
