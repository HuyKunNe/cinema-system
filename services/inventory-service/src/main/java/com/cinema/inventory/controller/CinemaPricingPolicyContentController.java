package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingPolicyContentRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyContentResponse;
import com.cinema.inventory.service.CinemaPricingPolicyContentService;

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
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaPricingPolicyContentController {

    private final CinemaPricingPolicyContentService contentService;

    public CinemaPricingPolicyContentController(CinemaPricingPolicyContentService contentService) {

        this.contentService = contentService;
    }

    @Operation(
            operationId = "getCinemaPricingPolicyContent",
            summary = "Get complete pricing policy content",
            description =
                    """
                    Requires inventory:manage.
                    Returns policy metadata, base price rules, holiday dates
                    and surcharge rules from one consistent policy version.
                    This operation does not validate publication readiness
                    or evaluate prices.
                    """)
    @GetMapping("/{policyId}/content")
    public ResponseEntity<CinemaPricingPolicyContentResponse> getContent(
            @PathVariable("cinemaId") UUID cinemaId, @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(contentService.getContent(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingPolicyContent",
            summary = "Replace all base price rules of a draft",
            description =
                    """
                     Requires inventory:manage.
                     expectedVersion refers to policy.version, not policyVersion.
                     Replaces every base price rule; an empty rules array clears only the base price rules.
                     Holiday dates and surcharge rules are preserved.
                     Rule IDs are regenerated.
                     Published policies cannot be modified.
                     Returns the complete policy content after saving.
                     This operation does not publish or evaluate ticket prices.
                    """)
    @PutMapping("/{policyId}/content")
    public ResponseEntity<CinemaPricingPolicyContentResponse> replaceContent(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaPricingPolicyContentRequest request) {

        return ResponseEntity.ok(contentService.replaceContent(cinemaId, policyId, request));
    }
}
