package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CheckDraftBasePriceCoverageRequest;
import com.cinema.inventory.dto.response.DraftBasePriceCoverageResponse;
import com.cinema.inventory.service.pricing.DraftBasePriceCoverageService;

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
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/base-price-coverage")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class DraftBasePriceCoverageController {

    private final DraftBasePriceCoverageService coverageService;

    public DraftBasePriceCoverageController(
            DraftBasePriceCoverageService coverageService) {

        this.coverageService = coverageService;
    }

    @Operation(
            operationId = "checkDraftBasePriceCoverage",
            summary = "Analyze draft base prices within one local-day window",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version.
                    startsAt identifies the local date in the policy time zone.
                    fromMinute and toMinute define a half-open local-time window.

                    Returns MATCHED, GAP or AMBIGUOUS intervals.
                    Membership and channel inputs are hypothetical.
                    Surcharges and customer eligibility are not evaluated.
                    This is wall-clock rule analysis, not a list of valid scheduling instants.
                    publicationValidated is always false.
                    No policy data is modified.
                    """)
    @PostMapping
    public ResponseEntity<DraftBasePriceCoverageResponse> check(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody CheckDraftBasePriceCoverageRequest request) {

        return ResponseEntity.ok(
                coverageService.check(cinemaId, policyId, request));
    }
}
