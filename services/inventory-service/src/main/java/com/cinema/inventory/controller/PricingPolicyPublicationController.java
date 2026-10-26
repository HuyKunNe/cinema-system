package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.PublishPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.dto.response.FareMatrixValidationResponse;
import com.cinema.inventory.service.PricingPolicyPublicationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/" + "{policyId}/versions/{versionId}")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class PricingPolicyPublicationController {

    private final PricingPolicyPublicationService service;

    public PricingPolicyPublicationController(PricingPolicyPublicationService service) {

        this.service = service;
    }

    @GetMapping("/fare-matrix/validation")
    public ResponseEntity<FareMatrixValidationResponse> validate(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId) {

        return ResponseEntity.ok(service.validate(cinemaId, policyId, versionId));
    }

    @PostMapping("/publish")
    public ResponseEntity<CinemaPricingPolicyVersionResponse> publish(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody PublishPricingPolicyRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                service.publish(cinemaId, policyId, versionId, request, authentication.getName()));
    }
}
