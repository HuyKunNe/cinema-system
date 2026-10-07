package com.cinema.inventory.controller;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.response.ShowtimeSeatMapResponse;
import com.cinema.inventory.service.ShowtimeSeatMapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/showtimes")
@Tag(name = "showtime-seat-map-controller")
public class ShowtimeSeatMapController {

    private final ShowtimeSeatMapService seatMapService;

    public ShowtimeSeatMapController(
            ShowtimeSeatMapService seatMapService) {

        this.seatMapService = seatMapService;
    }

    @Operation(
            operationId = "getShowtimeSeatMap",
            summary = "Get the pinned seat map of a showtime",
            description =
                    """
                    Public read API.

                    Geometry comes from the pinned published layout.
                    Price and status come from ShowSeat.
                    Missing ShowSeat yields null showSeatId, price and status,
                    with selectable=false.

                    selectable also requires the showtime to be eligible for booking.
                    HELD seats are not automatically released by this API.

                    No booking-owner information is returned.
                    Reading the map does not reserve seats.
                    """)
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Seat map",
                content = @Content(
                        schema = @Schema(
                                implementation = ShowtimeSeatMapResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid showtime UUID",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Showtime not found",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "Missing pinned layout, unpublished layout or inconsistent seat map",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class)))
    })
    @GetMapping("/{showtimeId}/seat-map")
    public ResponseEntity<ShowtimeSeatMapResponse> getShowtimeSeatMap(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatMapService.getSeatMap(showtimeId));
    }
}
