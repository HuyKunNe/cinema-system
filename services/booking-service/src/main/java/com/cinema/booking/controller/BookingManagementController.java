package com.cinema.booking.controller;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.service.BookingManagementReadService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.PageResponse;
import com.cinema.common.validation.annotation.UuidV5OrV7;
import com.cinema.common.validation.annotation.UuidV7;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/management")
@SecurityRequirement(name = "oauth2")
@Tag(name = "Booking management")
public class BookingManagementController {

    private final BookingManagementReadService bookingManagementReadService;

    public BookingManagementController(BookingManagementReadService bookingManagementReadService) {

        this.bookingManagementReadService = bookingManagementReadService;
    }

    @Operation(
            operationId = "listBookingsForAdmin",
            summary = "List bookings for administrators",
            description =
                    "Requires the ADMIN role. "
                            + "Filters are combined with AND. "
                            + "createdFrom is inclusive; createdTo is exclusive. "
                            + "Results are ordered by createdAt descending, then id descending.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> listBookingsForAdmin(
            @RequestParam(name = "status", required = false) BookingStatus status,
            @RequestParam(name = "showtimeId", required = false) @UuidV5OrV7 UUID showtimeId,
            @RequestParam(name = "userId", required = false) @UuidV7 UUID userId,
            @RequestParam(name = "createdFrom", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime createdFrom,
            @RequestParam(name = "createdTo", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime createdTo,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {

        PageResponse<BookingResponse> response =
                bookingManagementReadService.findAll(
                        status, showtimeId, userId, createdFrom, createdTo, page, size);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }

    @Operation(
            operationId = "getBookingForAdmin",
            summary = "Get booking details for administrators",
            description = "Requires the ADMIN role.")
    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingForAdmin(
            @PathVariable("bookingId") @UuidV7 UUID bookingId) {

        BookingResponse response = bookingManagementReadService.findById(bookingId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(response));
    }
}
