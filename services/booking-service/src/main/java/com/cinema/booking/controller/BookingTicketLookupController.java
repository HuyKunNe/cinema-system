package com.cinema.booking.controller;

import com.cinema.booking.service.BookingTicketService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/management/tickets/lookup")
public class BookingTicketLookupController {
    private final BookingTicketService service;

    public BookingTicketLookupController(BookingTicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingTicketService.LookupResponse>> lookupBookingTicket(
            @Valid @RequestBody LookupRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.lookup(request.qrPayload())));
    }

    public record LookupRequest(
            @NotBlank @Pattern(regexp = "^cinema-ticket:v1:[A-Za-z0-9_-]{43}$") String qrPayload) {
        @Override
        public String toString() {
            return "LookupRequest[qrPayload=REDACTED]";
        }
    }
}
