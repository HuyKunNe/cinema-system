package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CheckDraftFareMatrixCoverageRequest;
import com.cinema.inventory.dto.response.DraftFareMatrixCoverageResponse;
import com.cinema.inventory.service.pricing.DraftFareMatrixCoverageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/coverage")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class DraftFareMatrixCoverageController {

    private final DraftFareMatrixCoverageService coverageService;

    public DraftFareMatrixCoverageController(DraftFareMatrixCoverageService coverageService) {

        this.coverageService = coverageService;
    }

    @Operation(
            operationId = "checkDraftCinemaFareMatrixCoverage",
            summary = "Analyze draft fare coverage within one local-day window",
            description =
                    """
                    Requires inventory:manage.
                    Supports VND FARE_MATRIX_V2 drafts only.
                    scenario.expectedVersion refers to policy.version.
                    scenario.startsAt selects the date in the policy time zone.
                    fromMinute and toMinute define a half-open local clock window.

                    All other scenario conditions remain fixed across the window.
                    First-showing and eligibility inputs are hypothetical.
                    Returns MATCHED, GAP or AMBIGUOUS segments.
                    Equal-priority winners remain ambiguous even when prices match.
                    Flags cover the complete analysis; at most 200 merged segments return.

                    This is local wall-clock analysis, not validated scheduling instants.
                    Does not establish full policy coverage or publication readiness.
                    Does not verify eligibility, calculate additional surcharges,
                    produce a booking quote or modify stored data.
                    """)
    @ApiResponse(responseCode = "200", description = "Coverage simulation result")
    @ApiResponse(responseCode = "400", description = "Invalid scenario or time window")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description =
                    "Wrong model, non-draft policy, stale version "
                            + "or date outside policy period")
    @PostMapping
    public ResponseEntity<DraftFareMatrixCoverageResponse> check(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CheckDraftFareMatrixCoverageRequest request) {

        return ResponseEntity.ok(coverageService.check(cinemaId, policyId, request));
    }
}
