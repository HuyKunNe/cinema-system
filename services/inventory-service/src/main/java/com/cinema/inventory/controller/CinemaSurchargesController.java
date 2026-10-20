package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaSurchargesRequest;
import com.cinema.inventory.dto.response.CinemaSurchargesResponse;
import com.cinema.inventory.service.CinemaSurchargesService;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/surcharges")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class CinemaSurchargesController {

    private final CinemaSurchargesService surchargeService;

    public CinemaSurchargesController(
            CinemaSurchargesService surchargeService) {

        this.surchargeService = surchargeService;
    }

    @Operation(
            operationId = "getCinemaPricingPolicySurcharges",
            summary = "Get pricing policy surcharge rules",
            description = "Requires inventory:manage.")
    @GetMapping
    public ResponseEntity<CinemaSurchargesResponse> getSurcharges(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(
                surchargeService.getSurcharges(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingPolicySurcharges",
            summary = "Replace all surcharge rules of a draft pricing policy",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version, not policyVersion.
                    Replaces every surcharge rule; an empty array clears them.
                    unit, stackable and priority must be supplied explicitly.
                    Rule IDs are regenerated.
                    Published policies cannot be modified.
                    Base price rules and holiday dates are preserved.
                    This operation does not evaluate prices.
                    Movie attribute and showtime format codes are draft configuration;
                    their runtime support is not verified by this operation.
                    """)
    @PutMapping
    public ResponseEntity<CinemaSurchargesResponse> replaceSurcharges(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaSurchargesRequest request) {

        return ResponseEntity.ok(
                surchargeService.replaceSurcharges(
                        cinemaId, policyId, request));
    }
}
