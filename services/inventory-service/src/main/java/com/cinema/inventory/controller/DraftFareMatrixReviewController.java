package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaFareMatrixReviewResponse;
import com.cinema.inventory.service.pricing.DraftFareMatrixReviewService;

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
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/review")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class DraftFareMatrixReviewController {

    private final DraftFareMatrixReviewService reviewService;

    public DraftFareMatrixReviewController(DraftFareMatrixReviewService reviewService) {

        this.reviewService = reviewService;
    }

    @Operation(
            operationId = "reviewDraftCinemaFareMatrix",
            summary = "Review selected configuration issues of a draft fare matrix",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.

                    Reviews required data, fare units, effective periods,
                    unused dimensions, holiday configuration and ticket categories.
                    Reports empty category catalogs, unconfirmed category eligibility
                    and fares referencing unknown category codes.
                    Unused categories and unverified eligibility rules produce warnings.

                    Returns at most 200 issues.
                    hasErrors includes errors beyond the response limit.

                    Does not evaluate full policy coverage, highest-priority ambiguities,
                    category sale scope, eligibility rule implementations,
                    customer evidence, surcharge behavior or business confirmation.
                    publicationValidated is always false.
                    A report without errors does not authorize publication.
                    Does not modify the policy.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Review completed; configuration errors are included in the report")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PostMapping
    public ResponseEntity<CinemaFareMatrixReviewResponse> review(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReviewCinemaPricingPolicyRequest request) {

        return ResponseEntity.ok(reviewService.review(cinemaId, policyId, request));
    }
}
