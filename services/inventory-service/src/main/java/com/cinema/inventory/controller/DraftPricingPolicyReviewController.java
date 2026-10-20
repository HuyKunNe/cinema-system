package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyReviewResponse;
import com.cinema.inventory.service.pricing.DraftPricingPolicyReviewService;

import io.swagger.v3.oas.annotations.Operation;
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
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/review")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class DraftPricingPolicyReviewController {

    private final DraftPricingPolicyReviewService reviewService;

    public DraftPricingPolicyReviewController(
            DraftPricingPolicyReviewService reviewService) {

        this.reviewService = reviewService;
    }

    @Operation(
            operationId = "reviewDraftCinemaPricingPolicy",
            summary = "Review selected configuration issues of a draft policy",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version, not policyVersion.
                    Returns errors and warnings without modifying the policy.

                    Equal-priority overlaps are potential conflicts, not proven
                    highest-priority ambiguities.
                    Full price coverage and customer eligibility are not verified.
                    publicationValidated is always false.
                    A report without errors does not authorize publication.
                    """)
    @PostMapping
    public ResponseEntity<CinemaPricingPolicyReviewResponse> review(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReviewCinemaPricingPolicyRequest request) {

        return ResponseEntity.ok(
                reviewService.review(cinemaId, policyId, request));
    }
}
