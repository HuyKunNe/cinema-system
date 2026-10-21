package com.cinema.inventory.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.request.ReviewCinemaPricingPolicyRequest;
import com.cinema.inventory.dto.response.ValidateDraftFareMatrixResponse;
import com.cinema.inventory.service.pricing.DraftFareMatrixValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/fare-matrix/validate")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class DraftFareMatrixValidationController {

    private final DraftFareMatrixValidationService validationService;

    public DraftFareMatrixValidationController(DraftFareMatrixValidationService validationService) {
        this.validationService = validationService;
    }

    @Operation(
            operationId = "validateDraftCinemaFareMatrix",
            summary = "Validate draft fare coverage and priority determinism",
            description =
                    """
                    Requires inventory:manage.
                    Supports FARE_MATRIX_V2 drafts only.
                    expectedVersion refers to policy.version, not policyVersion.

                    Runs configuration review first.
                    Checks declared sale scopes across the policy effective period,
                    known holidays, weekdays, time boundaries and screening conditions.

                    Eligibility codes are independent positive predicates.
                    The current algorithm supports one required code per fare,
                    together with the category's required code.
                    It does not verify real customer eligibility or compatibility
                    between eligibility codes.

                    Returns at most 200 matrix findings.
                    COMPLETE means analysis finished, not necessarily valid.
                    ANALYSIS_LIMIT_REACHED never produces matrixValid=true.
                    Finding counts cover findings discovered before analysis stops.

                    publicationValidated is always false.
                    Does not evaluate surcharges, publish policies, create quotes,
                    validate scheduling instants or modify stored data.
                    """)
    @ApiResponse(responseCode = "200", description = "Validation report")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Policy not found in this cinema")
    @ApiResponse(
            responseCode = "409",
            description = "Wrong model, non-draft policy or stale expectedVersion")
    @PostMapping
    public ResponseEntity<ValidateDraftFareMatrixResponse> validateMatrix(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReviewCinemaPricingPolicyRequest request) {

        return ResponseEntity.ok(validationService.validateMatrix(cinemaId, policyId, request));
    }
}
