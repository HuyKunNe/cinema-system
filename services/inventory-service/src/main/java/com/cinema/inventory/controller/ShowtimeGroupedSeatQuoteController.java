package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.QuoteShowtimeSeatsRequest;
import com.cinema.inventory.dto.response.ShowtimeGroupedSeatQuoteResponse;
import com.cinema.inventory.service.ShowtimeGroupedSeatQuoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeGroupedSeatQuoteController {

    private final ShowtimeGroupedSeatQuoteService service;

    public ShowtimeGroupedSeatQuoteController(ShowtimeGroupedSeatQuoteService service) {

        this.service = service;
    }

    @Operation(
            operationId = "quoteShowtimeSeats",
            summary = "Quote selected seats using their requested ticket groups",
            description =
                    "Resolve published fares for each selected seat. "
                            + "The quote does not reserve seats or verify "
                            + "customer eligibility for a ticket group.")
    @SecurityRequirements
    @PostMapping("/{showtimeId}/seat-quote")
    public ResponseEntity<ShowtimeGroupedSeatQuoteResponse> quoteShowtimeSeats(
            @PathVariable("showtimeId") UUID showtimeId,
            @Valid @RequestBody QuoteShowtimeSeatsRequest request) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.quote(showtimeId, request));
    }
}
