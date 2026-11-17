package com.cinema.booking.controller;

import com.cinema.booking.service.TicketCheckInService;
import com.cinema.booking.service.TicketCheckInTransactionService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/management/tickets/check-in")
public class BookingTicketCheckInController {

    private final TicketCheckInService service;

    public BookingTicketCheckInController(TicketCheckInService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TicketCheckInTransactionService.Result>> checkInBookingTicket(
            @Valid @RequestBody BookingTicketLookupController.LookupRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ResponseFactory.success(
                                service.checkIn(request.qrPayload(), CurrentUser.id())));
    }
}
