package com.cinema.inventory.controller;

import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.api.pricing.CreateBookingQuoteRequest;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.inventory.service.pricing.BookingQuotePricingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/booking-quotes")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"booking:create"})
public class InventoryBookingQuoteController {

    private final BookingQuotePricingService service;

    public InventoryBookingQuoteController(BookingQuotePricingService service) {
        this.service = service;
    }

    @Operation(
            operationId = "createInventoryBookingQuote",
            summary = "Calculate and persist a policy-based pricing quote",
            description =
                    """
                    Requires booking:create.
                    Owner identity is derived from the authenticated user.

                    Uses the policy pinned to the showtime.
                    Amounts are all-inclusive integer VND strings.
                    COUPLE is charged once per pair.
                    Duplicate showSeatIds are rejected.

                    Does not hold seats or create a booking.
                    Scheduled showtimes return saleOpen=false.
                    RULE_REQUIRED conditions need registered trusted verifiers.
                    """)
    @PostMapping
    public ResponseEntity<BookingQuoteResponse> create(
            @Valid @RequestBody CreateBookingQuoteRequest request) {

        return ResponseEntity.ok(service.create(CurrentUser.id(), request));
    }
}
