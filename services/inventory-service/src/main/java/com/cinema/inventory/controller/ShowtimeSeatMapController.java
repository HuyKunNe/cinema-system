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

                    Positions, seat numbers and seat types come from the
                    showtime's pinned published layout.
                    Price and status come from ShowSeat.

                    When matrix is non-null:
                    - matrix contains the complete row and column counts.
                    - seats contain zero-based rowIndex and columnIndex.
                    - number is the consecutive seat number within its row.
                    - seatNumber is the display label, such as A1.
                    - columnSpan is 2 for COUPLE and 1 for other seat types.
                    - each COUPLE seat appears once at its starting cell.
                    - missing cells are empty spaces, not seats.
                    - the screen is displayed above the matrix.
                    - canvas dimensions and seat geometry are null.
                    - elements is an empty array.

                    When matrix is null, use the legacy canvas dimensions,
                    seat geometry and layout elements.

                    Missing ShowSeat yields null showSeatId, price and status,
                    with selectable=false.

                    selectable also requires the showtime to be eligible
                    for booking. HELD seats are not automatically released.

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
                                implementation =
                                        ShowtimeSeatMapResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid showtime UUID",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse
                                                .class))),
        @ApiResponse(
                responseCode = "404",
                description = "Showtime not found",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse
                                                .class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "Missing pinned layout, unpublished layout "
                                + "or inconsistent seat map",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse
                                                .class)))
    })
    @GetMapping("/{showtimeId}/seat-map")
    public ResponseEntity<ShowtimeSeatMapResponse> getShowtimeSeatMap(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatMapService.getSeatMap(showtimeId));
    }
}
