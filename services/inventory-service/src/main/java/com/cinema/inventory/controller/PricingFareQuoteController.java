package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.QuotePricingFareRequest;
import com.cinema.inventory.dto.response.PricingFareQuoteResponse;
import com.cinema.inventory.service.PricingFareQuoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
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
public class PricingFareQuoteController {

    private final PricingFareQuoteService service;

    public PricingFareQuoteController(PricingFareQuoteService service) {

        this.service = service;
    }

    @Operation(operationId = "quotePricingFare")
    @PostMapping("/quote")
    public ResponseEntity<PricingFareQuoteResponse> quote(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody QuotePricingFareRequest request) {

        return ResponseEntity.ok(service.quote(cinemaId, policyId, versionId, request));
    }
}
