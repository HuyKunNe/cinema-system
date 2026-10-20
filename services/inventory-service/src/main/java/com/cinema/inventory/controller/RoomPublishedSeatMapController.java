package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.RoomPublishedSeatMapResponse;
import com.cinema.inventory.service.RoomPublishedSeatMapService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/room-layouts")
@Tag(name = "room-layout-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
public class RoomPublishedSeatMapController {

    private final RoomPublishedSeatMapService seatMapService;

    public RoomPublishedSeatMapController(RoomPublishedSeatMapService seatMapService) {

        this.seatMapService = seatMapService;
    }

    @Operation(
            operationId = "getRoomPublishedSeatMap",
            summary = "Get a published room matrix for administration",
            description =
                    """
                    Requires inventory:manage.

                    roomId is required.
                    When layoutId is supplied, it must belong to roomId
                    and identify a PUBLISHED matrix layout.

                    When layoutId is omitted, selects the PUBLISHED layout
                    with the greatest layoutVersion in the room.
                    Draft layouts do not affect this selection.
                    A legacy geometry layout is rejected rather than converted.

                    Matrix dimensions preserve empty rows and columns.
                    Seat positions, numbers and types come from layout cells.
                    Active state comes from the inventory seat.

                    Each COUPLE seat appears once at its starting cell
                    with columnSpan=2 and capacity=2.
                    Missing cells are empty spaces, not inventory seats.
                    Display the screen above the matrix.

                    This API does not return prices or booking availability.
                    It does not change any showtime's pinned layout.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Published room matrix",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        RoomPublishedSeatMapResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid room or layout UUID",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "403",
                description = "Requires inventory:manage",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "404",
                description =
                        "Room not found, no published layout, "
                                + "or selected layout does not belong to the room",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "Selected layout is not published, "
                                + "is a legacy geometry layout, "
                                + "or contains inconsistent matrix data",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @GetMapping("/published-seat-map")
    public ResponseEntity<RoomPublishedSeatMapResponse> getSeatMap(
            @RequestParam("roomId") UUID roomId,
            @RequestParam(name = "layoutId", required = false) UUID layoutId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatMapService.getSeatMap(roomId, layoutId));
    }
}
