package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceCinemaHolidayDatesRequest;
import com.cinema.inventory.dto.response.CinemaHolidayDatesResponse;
import com.cinema.inventory.service.CinemaHolidayDatesService;

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
@RequestMapping("/api/v1/cinemas/{cinemaId}/pricing-policies/{policyId}/holiday-dates")
@Tag(name = "cinema-pricing-policy-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class CinemaHolidayDatesController {

    private final CinemaHolidayDatesService holidayService;

    public CinemaHolidayDatesController(CinemaHolidayDatesService holidayService) {

        this.holidayService = holidayService;
    }

    @Operation(
            operationId = "getCinemaPricingPolicyHolidayDates",
            summary = "Get holiday dates of a pricing policy",
            description = "Requires inventory:manage. Dates use the policy time zone.")
    @GetMapping
    public ResponseEntity<CinemaHolidayDatesResponse> getHolidayDates(
            @PathVariable("cinemaId") UUID cinemaId, @PathVariable("policyId") UUID policyId) {

        return ResponseEntity.ok(holidayService.getHolidayDates(cinemaId, policyId));
    }

    @Operation(
            operationId = "replaceCinemaPricingPolicyHolidayDates",
            summary = "Replace holiday dates of a draft pricing policy",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to policy.version, not policyVersion.
                    Dates must be unique and within the policy effective period.
                    An empty array clears the holiday calendar.
                    Published policies cannot be modified.
                    Base price rules are preserved.
                    This operation does not evaluate prices.
                    """)
    @PutMapping
    public ResponseEntity<CinemaHolidayDatesResponse> replaceHolidayDates(
            @PathVariable("cinemaId") UUID cinemaId,
            @PathVariable("policyId") UUID policyId,
            @Valid @RequestBody ReplaceCinemaHolidayDatesRequest request) {

        return ResponseEntity.ok(holidayService.replaceHolidayDates(cinemaId, policyId, request));
    }
}
