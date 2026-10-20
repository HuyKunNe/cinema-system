package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.service.CinemaPricingPolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaPricingPolicyController {

    private final CinemaPricingPolicyService policyService;

    public CinemaPricingPolicyController(CinemaPricingPolicyService policyService) {

        this.policyService = policyService;
    }

    @Operation(
            operationId = "createCinemaPricingPolicyDraft",
            summary = "Create a cinema pricing policy draft",
            description =
                    """
                    Requires inventory:manage.
                    The server assigns policyVersion and snapshots the cinema time zone.
                    The cinema must have a configured time zone.
                    Creates metadata only; no price rules are created or published.
                    Repeating a successful request creates another draft.
                    """)
    @ApiResponse(responseCode = "201", description = "Draft created")
    @PostMapping
    public ResponseEntity<CinemaPricingPolicyResponse> createDraft(
            @PathVariable("cinemaId") UUID cinemaId,
            @Valid @RequestBody CreateCinemaPricingPolicyRequest request) {

        CinemaPricingPolicyResponse response = policyService.createDraft(cinemaId, request);

        URI location =
                URI.create("/api/v1/cinemas/" + cinemaId + "/pricing-policies/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @Operation(
            operationId = "getCinemaPricingPolicies",
            summary = "List cinema pricing policy versions",
            description = "Requires inventory:manage. Returns newest policyVersion first.")
    @GetMapping
    public ResponseEntity<List<CinemaPricingPolicyResponse>> getByCinemaId(
            @PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(policyService.getByCinemaId(cinemaId));
    }

    @Operation(
            operationId = "getCinemaPricingPolicyById",
            summary = "Get cinema pricing policy metadata",
            description = "Requires inventory:manage.")
    @GetMapping("/{policyId}")
    public ResponseEntity<CinemaPricingPolicyResponse> getById(
            @PathVariable("cinemaId") UUID cinemaId, @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(policyService.getById(cinemaId, policyId));
    }
}
