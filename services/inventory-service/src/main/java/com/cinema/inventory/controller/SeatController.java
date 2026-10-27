package com.cinema.inventory.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.request.CreateSeatRangeRequest;
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatActiveRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatActiveStateResponse;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.service.SeatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    public ResponseEntity<SeatResponse> createSeat(
            @RequestParam("roomId") UUID roomId,
            @Valid @RequestBody CreateSeatRequest request) {

        SeatResponse response = seatService.create(roomId, request);

        return ResponseEntity.created(
                        URI.create("/api/v1/seats/" + response.id()))
                .body(response);
    }

    @PostMapping("/range")
    public ResponseEntity<List<SeatResponse>> createSeatRange(
            @RequestParam("roomId") UUID roomId,
            @Valid @RequestBody CreateSeatRangeRequest request) {

        List<SeatResponse> response =
                seatService.createRange(roomId, request);

        return ResponseEntity.created(
                        URI.create("/api/v1/seats?roomId=" + roomId))
                .body(response);
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> getSeatById(
            @PathVariable("seatId") UUID seatId) {

        return ResponseEntity.ok(seatService.getById(seatId));
    }

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getActiveSeats(
            @RequestParam("roomId") UUID roomId) {

        return ResponseEntity.ok(
                seatService.getActiveSeats(roomId));
    }

    @Operation(
            operationId = "getManagedSeats",
            summary = "List legacy catalog seats for inventory management",
            description =
                    """
                    Omit active to include both active and inactive catalog seats.
                    Layout-managed seats are read from the published room matrix API.
                    """,
            security = {
                @SecurityRequirement(
                        name = "oauth2",
                        scopes = {"inventory:manage"})
            })
    @GetMapping("/management")
    public ResponseEntity<List<SeatResponse>> getManagedSeats(
            @RequestParam("roomId") UUID roomId,
            @RequestParam(
                            name = "active",
                            required = false)
                    Boolean active) {

        return ResponseEntity.ok(
                seatService.getManagedSeats(roomId, active));
    }

    @PutMapping("/{seatId}")
    public ResponseEntity<SeatResponse> updateSeat(
            @PathVariable("seatId") UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request) {

        return ResponseEntity.ok(
                seatService.update(seatId, request));
    }

    @Operation(
            operationId = "updateSeatActiveState",
            summary = "Update only the inventory seat active state",
            description =
                    """
                    Requires inventory:manage.
                    Supports both legacy catalog and layout-managed seats.
                    The seat must belong to the supplied roomId.

                    Sets active to the requested value.
                    Repeating the same request leaves the same active state.

                    Does not require or modify seat numbers, row labels or types.
                    Does not modify layout cells or published snapshots.
                    The active state belongs to the physical seat and is shared
                    by every layout referencing the same seatId.

                    Does not change existing ShowSeat statuses or bookings.
                    For newly generated show seats, the existing generation
                    rules use the inventory seat's active state.

                    Reload the published room matrix after a successful update.
                    """,
            security = {
                @SecurityRequirement(
                        name = "oauth2",
                        scopes = {"inventory:manage"})
            })
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Current inventory seat active state",
                content = @Content(
                        schema = @Schema(
                                implementation = SeatActiveStateResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid UUID, missing roomId or missing active",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "Requires inventory:manage",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Seat not found in the supplied room",
                content = @Content(
                        schema = @Schema(
                                implementation =
                                        com.cinema.common.response.model.ApiResponse.class)))
    })
    @PatchMapping("/{seatId}/active")
    public ResponseEntity<SeatActiveStateResponse> updateActive(
            @PathVariable("seatId") UUID seatId,
            @Valid @RequestBody UpdateSeatActiveRequest request) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatService.updateActive(seatId, request));
    }
}
