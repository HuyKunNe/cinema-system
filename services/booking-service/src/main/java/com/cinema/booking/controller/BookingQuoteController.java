package com.cinema.booking.controller;

import com.cinema.booking.client.InventoryQuoteClient;
import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.common.api.pricing.BookingQuoteResponse;
import com.cinema.common.api.pricing.CreateBookingQuoteRequest;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/quote")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"booking:create"})
public class BookingQuoteController {

    private final InventoryQuoteClient client;
    private final BookingProperties bookingProperties;

    public BookingQuoteController(
            InventoryQuoteClient client, BookingProperties bookingProperties) {

        this.client = client;
        this.bookingProperties = bookingProperties;
    }

    @Operation(
            operationId = "createBookingQuote",
            summary = "Obtain a server-calculated ticket quote",
            description =
                    """
                    Requires booking:create.
                    Send showSeatId and ticketCategoryCode per inventory seat.
                    Prices, eligibility flags and totals are not accepted as inputs.

                    Quote does not hold seats or create a booking.
                    saleOpen=false means the showtime is not open for sale.
                    Monetary amounts are integer VND strings.
                    """)
    @PostMapping
    public ResponseEntity<ApiResponse<BookingQuoteResponse>> create(
            @Valid @RequestBody CreateBookingQuoteRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        if (request.items().size() > bookingProperties.maxSeatsPerBooking()) {
            throw new ValidationException(BookingErrorCode.TOO_MANY_SEATS);
        }

        return ResponseEntity.ok(ResponseFactory.success(client.create(request, authorization)));
    }

    @Operation(
            operationId = "getBookingQuote",
            summary = "Read the current user's unexpired quote snapshot",
            description =
                    """
                    Requires booking:create.
                    Returns the original quote without recalculating prices.
                    Missing or foreign-owned quotes return 404.
                    Expired quotes return 409.

                    saleOpen describes the state at quote issuance.
                    Reading a quote does not reserve seats or create a booking.
                    """)
    @GetMapping("/{quoteId}")
    public ResponseEntity<ApiResponse<BookingQuoteResponse>> get(
            @PathVariable UUID quoteId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {

        return ResponseEntity.ok(ResponseFactory.success(client.get(quoteId, authorization)));
    }
}
