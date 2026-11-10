package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreatePricingPolicyDraftRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.service.PricingPolicyDraftService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
public class PricingPolicyDraftController {

    private final PricingPolicyDraftService service;

    public PricingPolicyDraftController(PricingPolicyDraftService service) {

        this.service = service;
    }

    @PostMapping("/{policyId}/versions/{sourceVersionId}/draft")
    public ResponseEntity<CinemaPricingPolicyVersionResponse> createPricingPolicyDraftFromPublished(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("sourceVersionId") UUID sourceVersionId,
            @Valid @RequestBody CreatePricingPolicyDraftRequest request) {

        var result = service.create(cinemaId, policyId, sourceVersionId, request);

        var response = result.response();

        UUID responseCinemaId =
                response.cinemaIds().contains(cinemaId) ? cinemaId : response.cinemaIds().get(0);

        URI location =
                URI.create(
                        "/api/v1/cinemas/"
                                + responseCinemaId
                                + "/pricing-policies/"
                                + response.policyId()
                                + "/versions/"
                                + response.id());

        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(location)
                .body(response);
    }
}
