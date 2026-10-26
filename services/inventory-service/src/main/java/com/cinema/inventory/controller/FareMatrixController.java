package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceFareMatrixRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.service.FareMatrixService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
@RequestMapping(
        "/api/v1/cinemas/{cinemaId}/pricing-policies/"
                + "{policyId}/versions/{versionId}/fare-matrix")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class FareMatrixController {

    private final FareMatrixService service;

    public FareMatrixController(FareMatrixService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<FareMatrixResponse> get(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId) {

        return ResponseEntity.ok(service.get(cinemaId, policyId, versionId));
    }

    @PutMapping
    public ResponseEntity<FareMatrixResponse> replace(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody ReplaceFareMatrixRequest request) {

        return ResponseEntity.ok(
                service.replace(cinemaId, policyId, versionId, request));
    }
}
