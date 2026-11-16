package com.cinema.booking.controller;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingHistoryPeriod;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.booking.service.BookingService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;

    private final BookingCancellationService bookingCancellationService;

    public BookingController(
            BookingService bookingService, BookingCancellationService bookingCancellationService) {

        this.bookingService = bookingService;
        this.bookingCancellationService = bookingCancellationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        BookingResponse response = bookingService.create(CurrentUser.id(), request);

        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/bookings/" + response.id()))
                .body(ResponseFactory.success(response));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> findById(
            @PathVariable("bookingId") @UuidV7 UUID bookingId) {

        BookingResponse response = bookingService.findById(CurrentUser.id(), bookingId);

        return ResponseEntity.ok(ResponseFactory.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> findAllBookings(
            @RequestParam(name = "page", defaultValue = "0")
                    @Min(value = 0, message = "Page must be zero or greater")
                    int page,
            @RequestParam(name = "size", defaultValue = "20")
                    @Min(value = 1, message = "Page size must be greater than zero")
                    @Max(value = 100, message = "Page size must not exceed 100")
                    int size,
            @RequestParam(name = "status", required = false) List<@NotNull BookingStatus> statuses,
            @RequestParam(name = "period", required = false) BookingHistoryPeriod period) {

        PageResponse<BookingResponse> response =
                period != null
                        ? bookingService.findHistory(CurrentUser.id(), page, size, statuses, period)
                        : statuses == null || statuses.isEmpty()
                                ? bookingService.findAll(CurrentUser.id(), page, size)
                                : bookingService.findAllByStatuses(
                                        CurrentUser.id(), page, size, statuses);

        return ResponseEntity.ok(ResponseFactory.success(response));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable("bookingId") @UuidV7 UUID bookingId) {

        BookingResponse response = bookingCancellationService.cancel(CurrentUser.id(), bookingId);

        return ResponseEntity.ok(ResponseFactory.success(response));
    }
}
