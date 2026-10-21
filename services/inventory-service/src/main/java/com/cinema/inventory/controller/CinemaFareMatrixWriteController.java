package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaFareMatrixRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.service.CinemaFareMatrixWriteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaFareMatrixWriteController {

    private final CinemaFareMatrixWriteService matrixService;

    public CinemaFareMatrixWriteController(
            CinemaFareMatrixWriteService matrixService) {

        this.matrixService = matrixService;
    }

    @Operation(
            operationId = "replaceCinemaPricingPolicyFareMatrix",
            summary = "Replace a draft cinema fare matrix",
            description =
                    """
                    Requires inventory:manage.
                    Supports VND FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.
                    Atomically replaces all day groups, time bands and fare rules.
                    All three arrays are required; empty arrays clear those sections.
                    Each fare must reference dimension codes from this request.
                    Dimension and fare IDs are regenerated.
                    Holiday dates are preserved.
                    amountVnd must be a canonical integer VND JSON string.
                    Returns updated policy metadata with the new lock version.
                    Does not validate matrix coverage, overlaps, eligibility,
                    publication readiness or booking prices.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Matrix replaced; updated policy metadata returned")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request, dimensions, references or fare data")
    @ApiResponse(
            responseCode = "404",
            description = "Policy not found within the requested cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PutMapping("/{policyId}/fare-matrix")
    public ResponseEntity<CinemaPricingPolicyResponse> replaceMatrix(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaFareMatrixRequest request) {

        return ResponseEntity.ok(
                matrixService.replaceMatrix(cinemaId, policyId, request));
    }
}
