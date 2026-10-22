package com.cinema.inventory.controller;

import com.cinema.common.api.screening.ReplaceScreeningCapabilitiesRequest;
import com.cinema.inventory.dto.response.RoomScreeningCapabilitiesResponse;
import com.cinema.inventory.service.RoomScreeningCapabilitiesService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms/{roomId}/screening-capabilities")
@Tag(name = "room-screening-capabilities-controller")
public class RoomScreeningCapabilitiesController {

    private final RoomScreeningCapabilitiesService service;

    public RoomScreeningCapabilitiesController(RoomScreeningCapabilitiesService service) {

        this.service = service;
    }

    @Operation(
            operationId = "getRoomScreeningCapabilities",
            summary = "Get explicitly configured room screening formats",
            description =
                    """
                    configuredFormats contains formats enabled for this room.
                    supportedFormatOptions is the server's format vocabulary,
                    not the capability list of this room.

                    An empty configuredFormats list enables no combinations.
                    RoomType does not automatically determine these capabilities.
                    """)
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<RoomScreeningCapabilitiesResponse> get(
            @PathVariable("roomId") UUID roomId) {

        return ResponseEntity.ok(service.get(roomId));
    }

    @Operation(
            operationId = "replaceRoomScreeningCapabilities",
            summary = "Replace all configured room screening formats",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion refers to room.version.
                    Replaces the complete configured format set.
                    An empty set clears all declared capabilities.
                    Does not modify existing showtimes or layout snapshots.
                    """)
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    @PutMapping
    public ResponseEntity<RoomScreeningCapabilitiesResponse> replace(
            @PathVariable("roomId") UUID roomId,
            @Valid @RequestBody ReplaceScreeningCapabilitiesRequest request) {

        return ResponseEntity.ok(service.replace(roomId, request));
    }
}
