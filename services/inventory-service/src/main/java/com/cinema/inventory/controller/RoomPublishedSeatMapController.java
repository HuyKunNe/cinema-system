package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.RoomPublishedSeatMapResponse;
import com.cinema.inventory.service.RoomPublishedSeatMapService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/room-layouts")
public class RoomPublishedSeatMapController {

    private final RoomPublishedSeatMapService seatMapService;

    public RoomPublishedSeatMapController(RoomPublishedSeatMapService seatMapService) {

        this.seatMapService = seatMapService;
    }

    @GetMapping("/published-seat-map")
    public ResponseEntity<RoomPublishedSeatMapResponse> getRoomPublishedSeatMap(
            @RequestParam("roomId") UUID roomId,
            @RequestParam(name = "layoutId", required = false) UUID layoutId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatMapService.getSeatMap(roomId, layoutId));
    }
}
