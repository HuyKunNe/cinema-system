package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateRoomLayoutRequest;
import com.cinema.inventory.dto.request.PublishRoomLayoutRequest;
import com.cinema.inventory.dto.request.ReplaceRoomLayoutContentRequest;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse;
import com.cinema.inventory.dto.response.RoomLayoutResponse;
import com.cinema.inventory.service.RoomLayoutContentService;
import com.cinema.inventory.service.RoomLayoutPublicationService;
import com.cinema.inventory.service.RoomLayoutService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/room-layouts")
@Tag(name = "room-layout-controller")
@SecurityRequirement(
        name = "oauth2",
        scopes = {"inventory:manage"})
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
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
            description = "Room or layout not found",
            content =
                    @Content(
                            schema =
                                    @Schema(
                                            implementation =
                                                    com.cinema.common.response.model.ApiResponse
                                                            .class)))
})
public class RoomLayoutController {

    private final RoomLayoutService roomLayoutService;
    private final RoomLayoutContentService roomLayoutContentService;
    private final RoomLayoutPublicationService roomLayoutPublicationService;

    public RoomLayoutController(
            RoomLayoutService roomLayoutService,
            RoomLayoutContentService roomLayoutContentService,
            RoomLayoutPublicationService roomLayoutPublicationService) {

        this.roomLayoutService = roomLayoutService;
        this.roomLayoutContentService = roomLayoutContentService;
        this.roomLayoutPublicationService = roomLayoutPublicationService;
    }

    @Operation(
            operationId = "createRoomLayoutDraft",
            summary = "Create room layout draft",
            description =
                    """
                    Creates an empty DRAFT layout.
                    Send only roomId to create an empty 6x6 matrix.
                    An explicit matrix may specify rowCount and columnCount,
                    but its seats array must be empty.
                    Legacy canvasWidth and canvasHeight remain supported.
                    Legacy dimensions and matrix are mutually exclusive.
                    The backend assigns layoutVersion.
                    This operation does not publish or bind the layout to showtimes.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Draft created",
                content = @Content(schema = @Schema(implementation = RoomLayoutResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Room layout version limit reached",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PostMapping
    public ResponseEntity<RoomLayoutResponse> createDraft(
            @Valid @RequestBody CreateRoomLayoutRequest request) {

        RoomLayoutResponse response = roomLayoutService.createDraft(request);

        return ResponseEntity.created(URI.create("/api/v1/room-layouts/" + response.id()))
                .body(response);
    }

    @Operation(operationId = "getRoomLayoutById", summary = "Get room layout metadata")
    @ApiResponse(
            responseCode = "200",
            description = "Room layout metadata",
            content = @Content(schema = @Schema(implementation = RoomLayoutResponse.class)))
    @GetMapping("/{layoutId}")
    public ResponseEntity<RoomLayoutResponse> getById(@PathVariable("layoutId") UUID layoutId) {

        return ResponseEntity.ok(roomLayoutService.getById(layoutId));
    }

    @Operation(
            operationId = "getRoomLayoutsByRoom",
            summary = "Get room layout metadata by room",
            description =
                    """
                    Returns all layout versions, newest first.
                    Returns an empty array when the room has no layouts.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Room layout versions",
            content =
                    @Content(
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    RoomLayoutResponse.class))))
    @GetMapping
    public ResponseEntity<List<RoomLayoutResponse>> getByRoomId(
            @RequestParam("roomId") UUID roomId) {

        return ResponseEntity.ok(roomLayoutService.getByRoomId(roomId));
    }

    @Operation(
            operationId = "getRoomLayoutContent",
            summary = "Get room layout content",
            description =
                    """
                    Requires inventory:manage.
                    Matrix layouts expose matrix with dimensions and occupied cells.
                    Legacy layouts expose geometry in seats and elements.
                    Matrix cells are independent of booking availability.
                    """)
    @ApiResponse(
            responseCode = "200",
            description = "Room layout content",
            content = @Content(schema = @Schema(implementation = RoomLayoutContentResponse.class)))
    @GetMapping("/{layoutId}/content")
    public ResponseEntity<RoomLayoutContentResponse> getContent(
            @PathVariable("layoutId") UUID layoutId) {

        return ResponseEntity.ok(roomLayoutContentService.getContent(layoutId));
    }

    @Operation(
            operationId = "replaceRoomLayoutContent",
            summary = "Replace draft room layout content",
            description =
                    """
                    Requires inventory:manage.
                    Replaces content of a DRAFT layout.
                    Matrix layouts accept expectedVersion and matrix.
                    Legacy layouts accept expectedVersion, seats and elements.
                    The two representations are mutually exclusive.
                    expectedVersion refers to layout.version, not layoutVersion.
                    Saving matrix content does not create or modify catalog seats.
                    Legacy layouts cannot be converted in place.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Updated room layout content",
                content =
                        @Content(
                                schema =
                                        @Schema(implementation = RoomLayoutContentResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Layout is not DRAFT or expectedVersion is stale",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PutMapping("/{layoutId}/content")
    public ResponseEntity<RoomLayoutContentResponse> replaceContent(
            @PathVariable("layoutId") UUID layoutId,
            @Valid @RequestBody ReplaceRoomLayoutContentRequest request) {

        return ResponseEntity.ok(roomLayoutContentService.replaceContent(layoutId, request));
    }

    @Operation(
            operationId = "publishRoomLayout",
            summary = "Publish a validated room layout",
            description =
                    """
                    Requires inventory:manage.
                    expectedVersion must match the current version field.
                    Requires at least one seat and one SCREEN.
                    Validates rotated geometry inside the canvas.
                    Rejects seat-seat and seat-element overlaps.
                    Publishes this layout version without binding it to showtimes.
                    """)
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Layout published",
                content = @Content(schema = @Schema(implementation = RoomLayoutResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Layout is not DRAFT or expectedVersion is stale",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        com.cinema.common.response.model.ApiResponse
                                                                .class)))
    })
    @PostMapping("/{layoutId}/publish")
    public ResponseEntity<RoomLayoutResponse> publish(
            @PathVariable("layoutId") UUID layoutId,
            @Valid @RequestBody PublishRoomLayoutRequest request) {

        return ResponseEntity.ok(roomLayoutPublicationService.publish(layoutId, request));
    }
}
