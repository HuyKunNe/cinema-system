package com.cinema.inventory.controller;

import com.cinema.inventory.dto.request.CreateRoomRequest;
import com.cinema.inventory.dto.request.UpdateRoomRequest;
import com.cinema.inventory.dto.response.RoomResponse;
import com.cinema.inventory.service.RoomService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            @RequestParam("cinemaId") UUID cinemaId,
            @Valid @RequestBody CreateRoomRequest request) {

        RoomResponse response = roomService.create(cinemaId, request);

        return ResponseEntity.created(URI.create("/api/v1/rooms/" + response.id())).body(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable("roomId") UUID roomId) {

        return ResponseEntity.ok(roomService.getById(roomId));
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getActiveRooms(
            @RequestParam("cinemaId") UUID cinemaId) {

        return ResponseEntity.ok(roomService.getActiveRooms(cinemaId));
    }

    @GetMapping("/management")
    @SecurityRequirement(
            name = "oauth2",
            scopes = {"inventory:manage"})
    public ResponseEntity<List<RoomResponse>> getManagedRooms(
            @RequestParam("cinemaId") UUID cinemaId,
            @RequestParam(name = "active", required = false) Boolean active) {

        return ResponseEntity.ok(roomService.getManagedRooms(cinemaId, active));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable("roomId") UUID roomId, @Valid @RequestBody UpdateRoomRequest request) {

        return ResponseEntity.ok(roomService.update(roomId, request));
    }
}
