package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.PreviewDraftFareMatrixRequest;
import com.cinema.inventory.dto.response.PreviewDraftFareMatrixResponse;
import com.cinema.inventory.service.pricing.DraftFareMatrixPreviewService;

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
@RequestMapping(
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/preview")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class DraftFareMatrixPreviewController {

    private final DraftFareMatrixPreviewService previewService;

    public DraftFareMatrixPreviewController(
            DraftFareMatrixPreviewService previewService) {

        this.previewService = previewService;
    }

    @Operation(
            operationId = "previewDraftCinemaFareMatrix",
            summary = "Simulate one fare selection using a draft matrix",
            description =
                    """
                    Requires inventory:manage.
                    Supports VND FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.
                    Uses the policy time zone and holiday calendar.

                    First-showing and eligibility inputs are hypothetical.
                    Room, movie, physical seat compatibility and customer
                    eligibility are not verified.

                    Returns MATCHED, GAP or AMBIGUOUS with highest-priority winners.
                    Equal-priority winners remain ambiguous even when amounts match.
                    MATCHED does not confirm the fare unit or publication readiness.

                    Does not calculate additional surcharges, produce a booking
                    quote, authorize payment or modify stored configuration.
                    """)
    @ApiResponse(responseCode = "200", description = "Simulation result")
    @ApiResponse(responseCode = "400", description = "Invalid simulation input")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description =
                    "Wrong model, non-draft policy, stale version "
                            + "or date outside policy period")
    @PostMapping
    public ResponseEntity<PreviewDraftFareMatrixResponse> preview(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody PreviewDraftFareMatrixRequest request) {

        return ResponseEntity.ok(
                previewService.preview(cinemaId, policyId, request));
    }
}
