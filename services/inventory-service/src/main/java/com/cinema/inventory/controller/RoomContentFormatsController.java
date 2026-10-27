package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.ReplaceRoomContentFormatsRequest;
import com.cinema.inventory.dto.response.RoomContentFormatsResponse;
import com.cinema.inventory.service.RoomContentFormatsService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
@RequestMapping("/api/v1/rooms/{roomId}/content-formats")
public class RoomContentFormatsController {

    private final RoomContentFormatsService service;

    public RoomContentFormatsController(RoomContentFormatsService service) {

        this.service = service;
    }

    @GetMapping
    public ResponseEntity<RoomContentFormatsResponse> getRoomContentFormats(@PathVariable("roomId") UUID roomId) {

        return ResponseEntity.ok(service.get(roomId));
    }

    @PutMapping
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<RoomContentFormatsResponse> replaceRoomContentFormats(
            @PathVariable("roomId") UUID roomId,
            @Valid @RequestBody ReplaceRoomContentFormatsRequest request) {

        return ResponseEntity.ok(service.replace(roomId, request));
    }
}
