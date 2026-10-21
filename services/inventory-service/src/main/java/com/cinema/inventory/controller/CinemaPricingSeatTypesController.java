package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingSeatTypesRequest;
import com.cinema.inventory.dto.response.CinemaPricingSeatTypesResponse;
import com.cinema.inventory.service.CinemaPricingSeatTypesService;

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
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/seat-types")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class CinemaPricingSeatTypesController {

    private final CinemaPricingSeatTypesService seatTypeService;

    public CinemaPricingSeatTypesController(
            CinemaPricingSeatTypesService seatTypeService) {

        this.seatTypeService = seatTypeService;
    }

    @Operation(
            operationId = "getCinemaPricingSeatTypes",
            summary = "Get pricing seat definitions and physical mappings",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 policies only.
                    Returns stored definitions ordered by code.
                    Does not validate fares or publication readiness.
                    """)
    @ApiResponse(responseCode = "200", description = "Seat definitions returned")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(responseCode = "409", description = "Unsupported pricing model")
    @GetMapping
    public ResponseEntity<CinemaPricingSeatTypesResponse> getSeatTypes(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(
                seatTypeService.getSeatTypes(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingSeatTypes",
            summary = "Replace pricing seat definitions of a draft policy",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.
                    Replaces the entire seat type catalog.
                    An empty array clears the catalog.
                    Definition IDs are regenerated.

                    Codes must be unique after normalization.
                    Each physical seat type may map to at most one pricing code.
                    UNCONFIRMED requires a null physicalSeatType.
                    CONFIRMED requires a non-null physicalSeatType.

                    Fare rules, categories, dimensions and holidays are preserved.
                    Fare rule references are not checked by this operation.
                    A confirmed mapping does not confirm fare units,
                    pair charging, surcharge rules or publication readiness.
                    """)
    @ApiResponse(responseCode = "200", description = "Seat catalog replaced")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid data, duplicate code or duplicate physical mapping")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PutMapping
    public ResponseEntity<CinemaPricingSeatTypesResponse> replaceSeatTypes(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaPricingSeatTypesRequest request) {

        return ResponseEntity.ok(
                seatTypeService.replaceSeatTypes(cinemaId, policyId, request));
    }
}
