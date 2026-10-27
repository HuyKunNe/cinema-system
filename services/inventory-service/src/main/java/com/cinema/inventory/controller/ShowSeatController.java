package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.service.ShowSeatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/show-seats")
public class ShowSeatController {

    private final ShowSeatService showSeatService;

    public ShowSeatController(ShowSeatService showSeatService) {
        this.showSeatService = showSeatService;
    }

    @GetMapping("/{showSeatId}")
    public ResponseEntity<ShowSeatResponse> getShowSeatById(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.getById(showSeatId));
    }

    @GetMapping
    public ResponseEntity<List<ShowSeatResponse>> getShowSeatsByShowtime(
            @RequestParam("showtimeId") UUID showtimeId,
            @RequestParam(name = "availableOnly", defaultValue = "false") boolean availableOnly) {

        if (availableOnly) {
            return ResponseEntity.ok(showSeatService.getAvailableByShowtimeId(showtimeId));
        }

        return ResponseEntity.ok(showSeatService.getByShowtimeId(showtimeId));
    }

    @Operation(operationId = "holdShowSeat", summary = "Hold an available show seat")
    @PutMapping("/{showSeatId}/hold")
    public ResponseEntity<ShowSeatResponse> holdShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody HoldShowSeatRequest request) {

        return ResponseEntity.ok(showSeatService.hold(showSeatId, request));
    }

    @PutMapping("/{showSeatId}/book")
    public ResponseEntity<ShowSeatResponse> bookShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody ShowSeatBookingRequest request) {

        return ResponseEntity.ok(showSeatService.book(showSeatId, request));
    }

    @PutMapping("/{showSeatId}/release")
    public ResponseEntity<ShowSeatResponse> releaseShowSeat(
            @PathVariable("showSeatId") UUID showSeatId,
            @Valid @RequestBody ShowSeatBookingRequest request) {

        return ResponseEntity.ok(showSeatService.release(showSeatId, request));
    }

    @Operation(
            operationId = "makeShowSeatUnavailable",
            summary = "Make an unreserved show seat unavailable",
            description =
                    """
                    Requires inventory:manage.

                    AVAILABLE changes to UNAVAILABLE.
                    An already UNAVAILABLE seat returns its current state.

                    HELD and BOOKED seats are rejected without changing
                    reservation ownership or expiration.

                    An expired HELD seat must first be released through
                    the reservation lifecycle.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Seat is unavailable, including repeated requests",
                content = @Content(schema = @Schema(implementation = ShowSeatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid UUID"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient authority"),
        @ApiResponse(responseCode = "404", description = "Show seat not found"),
        @ApiResponse(
                responseCode = "409",
                description = "Seat is HELD or BOOKED",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PutMapping("/{showSeatId}/unavailable")
    public ResponseEntity<ShowSeatResponse> makeUnavailable(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.makeUnavailable(showSeatId));
    }

    @Operation(
            operationId = "makeShowSeatAvailable",
            summary = "Make an unreserved show seat available",
            description =
                    """
                    Requires inventory:manage.

                    UNAVAILABLE changes to AVAILABLE.
                    An already AVAILABLE seat returns its current state.

                    HELD and BOOKED seats are rejected without changing
                    reservation ownership or expiration.

                    This operation does not cancel a booking
                    or release a reservation.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Seat is available, including repeated requests",
                content = @Content(schema = @Schema(implementation = ShowSeatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid UUID"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Insufficient authority"),
        @ApiResponse(responseCode = "404", description = "Show seat not found"),
        @ApiResponse(
                responseCode = "409",
                description = "Seat is HELD or BOOKED",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PutMapping("/{showSeatId}/available")
    public ResponseEntity<ShowSeatResponse> makeAvailable(
            @PathVariable("showSeatId") UUID showSeatId) {

        return ResponseEntity.ok(showSeatService.makeAvailable(showSeatId));
    }
}
