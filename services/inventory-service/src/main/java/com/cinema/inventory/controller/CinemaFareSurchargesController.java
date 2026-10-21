package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaFareSurchargesRequest;
import com.cinema.inventory.dto.response.CinemaFareSurchargesResponse;
import com.cinema.inventory.service.CinemaFareSurchargesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/surcharges")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class CinemaFareSurchargesController {

    private final CinemaFareSurchargesService service;

    public CinemaFareSurchargesController(CinemaFareSurchargesService service) {
        this.service = service;
    }

    @Operation(
            operationId = "getCinemaFareSurcharges",
            summary = "Get V2 fare surcharge configuration")
    @ApiResponse(responseCode = "200", description = "Configuration returned")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(responseCode = "409", description = "Unsupported pricing model")
    @GetMapping
    public ResponseEntity<CinemaFareSurchargesResponse> getSurcharges(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(service.getSurcharges(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaFareSurcharges",
            summary = "Replace V2 draft fare surcharge configuration",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version, not policyVersion.
                    Replaces the mode and complete surcharge list.
                    NONE requires an empty list.
                    UNCONFIRMED may be stored while editing a draft.
                    Rule IDs are regenerated.

                    Amounts are integer VND strings.
                    Fare references are reviewed separately.
                    Movie attribute and eligibility codes are declarations.
                    Does not calculate prices, verify conditions or authorize publication.
                    """)
    @ApiResponse(responseCode = "200", description = "Configuration replaced")
    @ApiResponse(responseCode = "400", description = "Invalid or duplicate data")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PutMapping
    public ResponseEntity<CinemaFareSurchargesResponse> replaceSurcharges(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaFareSurchargesRequest request) {

        return ResponseEntity.ok(
                service.replaceSurcharges(cinemaId, policyId, request));
    }
}
