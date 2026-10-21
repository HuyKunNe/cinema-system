package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaPricingTicketCategoriesRequest;
import com.cinema.inventory.dto.response.CinemaPricingTicketCategoriesResponse;
import com.cinema.inventory.service.CinemaPricingTicketCategoriesService;

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
@RequestMapping(
        "/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/ticket-categories")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(name = "oauth2", scopes = {"inventory:manage"})
public class CinemaPricingTicketCategoriesController {

    private final CinemaPricingTicketCategoriesService categoryService;

    public CinemaPricingTicketCategoriesController(
            CinemaPricingTicketCategoriesService categoryService) {

        this.categoryService = categoryService;
    }

    @Operation(
            operationId = "getCinemaPricingTicketCategories",
            summary = "Get ticket category definitions of a pricing policy",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 policies only.
                    Returns stored category definitions and policy metadata.
                    Does not verify customer eligibility or publication readiness.
                    """)
    @ApiResponse(responseCode = "200", description = "Category definitions returned")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(responseCode = "409", description = "Unsupported pricing model")
    @GetMapping
    public ResponseEntity<CinemaPricingTicketCategoriesResponse> getCategories(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(
                categoryService.getCategories(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingTicketCategories",
            summary = "Replace ticket categories of a draft pricing policy",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.
                    Replaces the complete category catalog.
                    An empty array clears the catalog.
                    Category IDs are regenerated.
                    Returns the catalog with updated policy metadata.

                    Fare rules, dimensions and holiday dates are preserved.
                    Category references in fare rules are not checked by this operation.
                    Eligibility rule codes are declarations, not verification results.
                    Does not evaluate prices or authorize publication.
                    """)
    @ApiResponse(responseCode = "200", description = "Category catalog replaced")
    @ApiResponse(responseCode = "400", description = "Invalid or duplicate category data")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PutMapping
    public ResponseEntity<CinemaPricingTicketCategoriesResponse> replaceCategories(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaPricingTicketCategoriesRequest request) {

        return ResponseEntity.ok(
                categoryService.replaceCategories(cinemaId, policyId, request));
    }
}
