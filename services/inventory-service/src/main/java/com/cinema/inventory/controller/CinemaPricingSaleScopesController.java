package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingSaleScopesRequest;
import com.cinema.inventory.dto.response.CinemaPricingSaleScopesResponse;
import com.cinema.inventory.service.CinemaPricingSaleScopesService;

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
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/sale-scopes")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaPricingSaleScopesController {

    private final CinemaPricingSaleScopesService service;

    public CinemaPricingSaleScopesController(CinemaPricingSaleScopesService service) {
        this.service = service;
    }

    @Operation(
            operationId = "getCinemaPricingSaleScopes",
            summary = "Get declared sale scopes of a V2 pricing policy")
    @ApiResponse(responseCode = "200", description = "Sale scopes returned")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(responseCode = "409", description = "Unsupported pricing model")
    @GetMapping
    public ResponseEntity<CinemaPricingSaleScopesResponse> getScopes(
            @PathVariable("cinemaId") UUID cinemaId, @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(service.getScopes(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingSaleScopes",
            summary = "Replace sale scopes of a V2 draft",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version, not policyVersion.
                    Replaces all sale scopes. An empty array clears the catalog.
                    IDs are regenerated. Duplicate codes or dimension tuples are rejected.
                    References are reviewed separately; incomplete drafts may be saved.
                    Fare rules and other catalogs are preserved.
                    Does not validate full coverage or authorize publication.
                    """)
    @ApiResponse(responseCode = "200", description = "Sale scopes replaced")
    @ApiResponse(responseCode = "400", description = "Invalid or duplicate scope data")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale version")
    @PutMapping
    public ResponseEntity<CinemaPricingSaleScopesResponse> replaceScopes(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaPricingSaleScopesRequest request) {

        return ResponseEntity.ok(service.replaceScopes(cinemaId, policyId, request));
    }
}
