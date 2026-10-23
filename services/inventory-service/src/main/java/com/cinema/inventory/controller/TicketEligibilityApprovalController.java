package com.cinema.inventory.controller;

import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.inventory.dto.request.CreateTicketEligibilityApprovalRequest;
import com.cinema.inventory.dto.response.TicketEligibilityApprovalResponse;
import com.cinema.inventory.service.pricing.ManualTicketEligibilityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eligibility-approvals")
public class TicketEligibilityApprovalController {

    private final ManualTicketEligibilityService service;

    public TicketEligibilityApprovalController(
            ManualTicketEligibilityService service) {
        this.service = service;
    }

    @Operation(
            operationId = "createTicketEligibilityApproval",
            summary = "Record staff verification for one attendee and seat",
            description = """
                    Requires inventory:manage.
                    Staff identity comes from the authenticated user.
                    userId identifies the purchaser who will own the quote.
                    The attendee may be another person.

                    Supports CATEGORY_DISCOUNT categories only.
                    COUPLE seats are rejected.
                    Does not calculate prices or hold seats.
                    verificationReference is an internal review reference,
                    not a raw identity-document number.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    @PostMapping
    public ResponseEntity<TicketEligibilityApprovalResponse> create(
            @Valid
            @RequestBody CreateTicketEligibilityApprovalRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(CurrentUser.id(), request));
    }

    @Operation(
            operationId = "getTicketEligibilityApproval",
            summary = "Read an eligibility approval owned by the current user",
            description = """
                    Requires booking:create.
                    Missing and foreign-owned approvals both return 404.
                    An expired approval may be read for reference,
                    but cannot establish eligibility for a new quote.
                    Internal verification references are not returned.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"booking:create"})
    @GetMapping("/{approvalId}")
    public ResponseEntity<TicketEligibilityApprovalResponse> get(
            @PathVariable UUID approvalId) {

        return ResponseEntity.ok(
                service.get(CurrentUser.id(), approvalId));
    }
}
