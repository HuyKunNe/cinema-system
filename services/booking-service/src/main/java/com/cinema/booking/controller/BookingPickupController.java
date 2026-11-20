package com.cinema.booking.controller;

import com.cinema.booking.dto.request.BookingPickupLookupRequest;
import com.cinema.booking.dto.response.BookingPickupLookupResponse;
import com.cinema.booking.dto.response.BookingPickupPassResponse;
import com.cinema.booking.service.BookingPickupService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.Valid;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
public class BookingPickupController {

    private final BookingPickupService service;

    public BookingPickupController(BookingPickupService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/bookings/{bookingId}/pickup-pass")
    public ResponseEntity<ApiResponse<BookingPickupPassResponse>> getBookingPickupPass(
            @PathVariable @UuidV7 UUID bookingId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.owner(CurrentUser.id(), bookingId)));
    }

    @PostMapping("/api/v1/bookings/pickups/lookup")
    public ResponseEntity<ApiResponse<BookingPickupLookupResponse>> lookupBookingPickup(
            @Valid @RequestBody BookingPickupLookupRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.lookup(request.qrPayload())));
    }
}
