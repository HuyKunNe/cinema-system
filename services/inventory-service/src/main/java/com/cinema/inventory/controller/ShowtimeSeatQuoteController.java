package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.ShowtimeSeatQuoteResponse;
import com.cinema.inventory.service.ShowtimeSeatQuoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeSeatQuoteController {

    private final ShowtimeSeatQuoteService service;

    public ShowtimeSeatQuoteController(ShowtimeSeatQuoteService service) {

        this.service = service;
    }

    @Operation(
            operationId = "getShowtimeSeatQuote",
            summary = "Quote currently available seats",
            description =
                    "Return stored seat prices and their total. "
                            + "Availability is checked when reading; "
                            + "the quote does not reserve seats.")
    @SecurityRequirements
    @GetMapping("/{showtimeId}/seat-quote")
    public ResponseEntity<ShowtimeSeatQuoteResponse> getShowtimeSeatQuote(
            @PathVariable("showtimeId") UUID showtimeId,
            @RequestParam("seatNumbers") List<String> seatNumbers) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.quote(showtimeId, seatNumbers));
    }
}
