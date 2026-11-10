package com.cinema.inventory.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.request.CopyPublishedRoomLayoutRequest;
import com.cinema.inventory.dto.request.CreateRoomLayoutRequest;
import com.cinema.inventory.dto.request.PublishRoomLayoutRequest;
import com.cinema.inventory.dto.request.ReplaceRoomLayoutContentRequest;
import com.cinema.inventory.dto.response.RoomLayoutContentResponse;
import com.cinema.inventory.dto.response.RoomLayoutResponse;
import com.cinema.inventory.service.RoomLayoutContentService;
import com.cinema.inventory.service.RoomLayoutDraftCopyService;
import com.cinema.inventory.service.RoomLayoutPublicationService;
import com.cinema.inventory.service.RoomLayoutService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/room-layouts")
public class RoomLayoutController {

    private final RoomLayoutService roomLayoutService;
    private final RoomLayoutContentService roomLayoutContentService;
    private final RoomLayoutPublicationService roomLayoutPublicationService;
    private final RoomLayoutDraftCopyService roomLayoutDraftCopyService;

    public RoomLayoutController(
            RoomLayoutService roomLayoutService,
            RoomLayoutContentService roomLayoutContentService,
            RoomLayoutPublicationService roomLayoutPublicationService,
            RoomLayoutDraftCopyService roomLayoutDraftCopyService) {

        this.roomLayoutService = roomLayoutService;
        this.roomLayoutContentService = roomLayoutContentService;
        this.roomLayoutPublicationService = roomLayoutPublicationService;
        this.roomLayoutDraftCopyService = roomLayoutDraftCopyService;
    }

    @PostMapping
    public ResponseEntity<RoomLayoutResponse> createRoomLayoutDraft(
            @Valid @RequestBody CreateRoomLayoutRequest request) {

        RoomLayoutResponse response = roomLayoutService.createDraft(request);

        return ResponseEntity.created(
                        URI.create("/api/v1/room-layouts/" + response.id()))
                .body(response);
    }

    @PostMapping("/{layoutId}/drafts")
    public ResponseEntity<RoomLayoutContentResponse> createRoomLayoutDraftFromPublished(
            @PathVariable("layoutId") UUID layoutId,
            @Valid @RequestBody CopyPublishedRoomLayoutRequest request) {

        RoomLayoutContentResponse response =
                roomLayoutDraftCopyService.copy(layoutId, request);

        return ResponseEntity.created(
                        URI.create(
                                "/api/v1/room-layouts/"
                                        + response.layout().id()))
                .body(response);
    }

    @GetMapping("/{layoutId}")
    public ResponseEntity<RoomLayoutResponse> getRoomLayoutById(
            @PathVariable("layoutId") UUID layoutId) {

        return ResponseEntity.ok(roomLayoutService.getById(layoutId));
    }

    @GetMapping
    public ResponseEntity<List<RoomLayoutResponse>> getRoomLayoutsByRoom(
            @RequestParam("roomId") UUID roomId) {

        return ResponseEntity.ok(roomLayoutService.getByRoomId(roomId));
    }

    @GetMapping("/{layoutId}/content")
    public ResponseEntity<RoomLayoutContentResponse> getRoomLayoutContent(
            @PathVariable("layoutId") UUID layoutId) {

        return ResponseEntity.ok(
                roomLayoutContentService.getContent(layoutId));
    }

    @PutMapping("/{layoutId}/content")
    public ResponseEntity<RoomLayoutContentResponse> replaceRoomLayoutContent(
            @PathVariable("layoutId") UUID layoutId,
            @Valid @RequestBody ReplaceRoomLayoutContentRequest request) {

        return ResponseEntity.ok(
                roomLayoutContentService.replaceContent(layoutId, request));
    }

    @PostMapping("/{layoutId}/publish")
    public ResponseEntity<RoomLayoutResponse> publishRoomLayout(
            @PathVariable("layoutId") UUID layoutId,
            @Valid @RequestBody PublishRoomLayoutRequest request) {

        return ResponseEntity.ok(
                roomLayoutPublicationService.publish(layoutId, request));
    }
}
