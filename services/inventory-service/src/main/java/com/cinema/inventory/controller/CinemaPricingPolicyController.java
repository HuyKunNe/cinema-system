package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.request.ReplacePricingPolicyDraftRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyVersionResponse;
import com.cinema.inventory.service.CinemaPricingPolicyService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaPricingPolicyController {

    private final CinemaPricingPolicyService service;

    public CinemaPricingPolicyController(CinemaPricingPolicyService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CinemaPricingPolicyVersionResponse> create(
            @PathVariable("cinemaId") UUID cinemaId,
            @Valid @RequestBody CreateCinemaPricingPolicyRequest request) {

        var result = service.create(cinemaId, request);
        var response = result.response();

        URI location =
                URI.create(
                        "/api/v1/cinemas/"
                                + cinemaId
                                + "/pricing-policies/"
                                + response.policyId()
                                + "/versions/"
                                + response.id());

        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .location(location)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<CinemaPricingPolicyVersionResponse>> listLatest(
            @PathVariable("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(service.listLatest(cinemaId));
    }

    @GetMapping("/{policyId}/versions")
    public ResponseEntity<List<CinemaPricingPolicyVersionResponse>> listVersions(
            @PathVariable("cinemaId") UUID cinemaId, @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(service.listVersions(cinemaId, policyId));
    }

    @GetMapping("/{policyId}/versions/{versionId}")
    public ResponseEntity<CinemaPricingPolicyVersionResponse> get(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId) {

        return ResponseEntity.ok(service.get(cinemaId, policyId, versionId));
    }

    @PutMapping("/{policyId}/versions/{versionId}")
    public ResponseEntity<CinemaPricingPolicyVersionResponse> replaceDraft(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @PathVariable("versionId") UUID versionId,
            @Valid @RequestBody ReplacePricingPolicyDraftRequest request) {

        return ResponseEntity.ok(service.replaceDraft(cinemaId, policyId, versionId, request));
    }
}
