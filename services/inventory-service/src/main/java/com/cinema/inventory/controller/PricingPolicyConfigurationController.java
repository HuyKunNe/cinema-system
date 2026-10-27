package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplacePricingConfigurationRequest;
import com.cinema.inventory.dto.response.FareMatrixResponse;
import com.cinema.inventory.service.PricingPolicyConfigurationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/cinemas/{cinemaId}/pricing-policies/"
                + "{policyId}/versions/{versionId}/configuration")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class PricingPolicyConfigurationController {

    private final PricingPolicyConfigurationService service;

    public PricingPolicyConfigurationController(PricingPolicyConfigurationService service) {

        this.service = service;
    }

    @Operation(operationId = "replacePricingPolicyConfiguration")
    @PutMapping
    public ResponseEntity<FareMatrixResponse> replace(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody ReplacePricingConfigurationRequest request) {

        return ResponseEntity.ok(service.replace(cinemaId, policyId, versionId, request));
    }
}
