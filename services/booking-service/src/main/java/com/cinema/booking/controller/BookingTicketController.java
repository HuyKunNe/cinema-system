package com.cinema.booking.controller;

import com.cinema.booking.service.BookingTicketService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/tickets")
public class BookingTicketController {
    private final BookingTicketService service;

    public BookingTicketController(BookingTicketService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingTicketService.TicketResponse>>> getBookingTickets(
            @PathVariable @UuidV7 UUID bookingId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.read(CurrentUser.id(), bookingId)));
    }
}
