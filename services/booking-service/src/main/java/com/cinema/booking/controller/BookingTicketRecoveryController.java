package com.cinema.booking.controller;

import com.cinema.booking.service.BookingTicketService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/management/{bookingId}/tickets/issue")
public class BookingTicketRecoveryController {
    private final BookingTicketService service;

    public BookingTicketRecoveryController(BookingTicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingTicketService.RecoveryResult>> recoverBookingTickets(
            @PathVariable @UuidV7 UUID bookingId,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ResponseFactory.success(
                                service.recover(bookingId, CurrentUser.id(), dryRun)));
    }
}
