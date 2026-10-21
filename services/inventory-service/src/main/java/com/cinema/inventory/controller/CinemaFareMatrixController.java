package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.CinemaFareMatrixResponse;
import com.cinema.inventory.service.CinemaFareMatrixService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaFareMatrixController {

    private final CinemaFareMatrixService matrixService;

    public CinemaFareMatrixController(CinemaFareMatrixService matrixService) {
        this.matrixService = matrixService;
    }

    @Operation(
            operationId = "getCinemaPricingPolicyFareMatrix",
            summary = "Get stored cinema fare matrix configuration",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 policies only.
                    Returns policy metadata, day groups, time bands,
                    fare rules and policy holiday dates.
                    amountVnd is an integer VND amount encoded as a decimal string.
                    policy.version is the optimistic-lock token;
                    policy.policyVersion is the business policy version.
                    Empty arrays represent missing configuration, not free tickets.
                    Does not evaluate fares, verify eligibility,
                    validate publication readiness or produce a booking quote.
                    Does not include a V2 surcharge model.
                    """)
    @ApiResponse(responseCode = "200", description = "Matrix configuration returned")
    @ApiResponse(
            responseCode = "404",
            description = "Policy not found within the requested cinema")
    @ApiResponse(
            responseCode = "409",
            description = "This endpoint supports FARE_MATRIX_V2 policies only")
    @GetMapping("/{policyId}/fare-matrix")
    public ResponseEntity<CinemaFareMatrixResponse> getMatrix(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(matrixService.getMatrix(cinemaId, policyId));
    }
}
