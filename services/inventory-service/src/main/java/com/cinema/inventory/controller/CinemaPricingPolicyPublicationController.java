package com.cinema.inventory.controller;

import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.inventory.dto.request.PublishCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.CinemaPricingPolicyResponse;
import com.cinema.inventory.dto.response.PublishCinemaPricingPolicyResponse;
import com.cinema.inventory.service.CinemaPricingPolicyPublicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaPricingPolicyPublicationController {

    private final CinemaPricingPolicyPublicationService publicationService;

    public CinemaPricingPolicyPublicationController(
            CinemaPricingPolicyPublicationService publicationService) {

        this.publicationService = publicationService;
    }

    @Operation(
            operationId = "publishCinemaPricingPolicy",
            summary = "Publish a confirmed all-inclusive fare matrix",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.

                    Locks cinema and policy, checks published-period conflicts,
                    and re-runs full matrix validation before publication.

                    Business confirmation and its reference are required.
                    Publisher identity comes from the authenticated user.

                    Returns published=false with validation details and HTTP 422
                    when matrix analysis fails or reaches its resource limit.

                    Published configuration is immutable.
                    This does not verify an actual customer's eligibility,
                    create a showtime, calculate a booking quote or open sales.
                    """)
    @ApiResponse(responseCode = "200", description = "Policy published")
    @ApiResponse(responseCode = "400", description = "Invalid publication confirmation")
    @ApiResponse(responseCode = "404", description = "Cinema or policy not found")
    @ApiResponse(
            responseCode = "409",
            description = "Stale version, wrong model/status or overlapping published period")
    @ApiResponse(
            responseCode = "422",
            description = "Publication rejected with matrix validation details")
    @PostMapping("/{policyId}/publish")
    public ResponseEntity<PublishCinemaPricingPolicyResponse> publish(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody PublishCinemaPricingPolicyRequest request) {

        PublishCinemaPricingPolicyResponse response =
                publicationService.publish(cinemaId, policyId, CurrentUser.id(), request);

        HttpStatus status = response.published() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;

        return ResponseEntity.status(status).body(response);
    }

    @Operation(
            operationId = "getActiveCinemaPricingPolicy",
            summary = "Resolve published policy for a showtime start",
            description =
                    """
                    Requires inventory:manage.
                    startsAt must be an ISO-8601 timestamp with offset.
                    Resolves the policy effective at that instant.

                    Each policy's effective dates use its stored time zone.
                    effectiveFrom is inclusive; effectiveTo is exclusive.
                    Drafts and policyVersion ordering do not determine selection.

                    Returns confirmed FARE_MATRIX_V2 metadata only.
                    Does not calculate fares or bind a policy to a showtime.
                    """)
    @ApiResponse(responseCode = "200", description = "Published policy resolved")
    @ApiResponse(responseCode = "400", description = "Invalid timestamp")
    @ApiResponse(responseCode = "404", description = "Cinema not found")
    @ApiResponse(
            responseCode = "409",
            description = "No matching policy, ambiguous policies or invalid publication")
    @GetMapping("/active")
    public ResponseEntity<CinemaPricingPolicyResponse> getActive(
            @PathVariable("cinemaId") UUID cinemaId,
            @RequestParam("startsAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime startsAt) {

        return ResponseEntity.ok(publicationService.getActive(cinemaId, startsAt));
    }
}
