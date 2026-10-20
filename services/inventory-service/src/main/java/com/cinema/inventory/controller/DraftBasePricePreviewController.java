package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.PreviewDraftBasePriceRequest;
import com.cinema.inventory.dto.response.PreviewDraftBasePriceResponse;
import com.cinema.inventory.service.pricing.DraftBasePricePreviewService;

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
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/base-price-preview")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class DraftBasePricePreviewController {

    private final DraftBasePricePreviewService previewService;

    public DraftBasePricePreviewController(DraftBasePricePreviewService previewService) {

        this.previewService = previewService;
    }

    @Operation(
            operationId = "previewDraftCinemaBasePrice",
            summary = "Simulate one base ticket price using a draft policy",
            description =
                    """
                    Requires inventory:manage.
                    The policy must be DRAFT.
                    expectedVersion refers to policy.version, not policyVersion.

                    Uses the policy time zone and holiday calendar.
                    Membership and channel values are hypothetical simulation inputs.
                    Customer eligibility is not verified.
                    Surcharges are not included.
                    The result is not a booking quote and cannot authorize payment.

                    This operation does not modify the policy.
                    """)
    @ApiResponse(responseCode = "200", description = "Base price simulation")
    @ApiResponse(responseCode = "400", description = "Invalid simulation input")
    @ApiResponse(responseCode = "401", description = "Authentication required")
    @ApiResponse(responseCode = "403", description = "Requires inventory:manage")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description =
                    "Stale version, policy not draft, date outside effective period, "
                            + "no matching rule or ambiguous highest-priority rules")
    @PostMapping
    public ResponseEntity<PreviewDraftBasePriceResponse> preview(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody PreviewDraftBasePriceRequest request) {

        return ResponseEntity.ok(previewService.preview(cinemaId, policyId, request));
    }
}
