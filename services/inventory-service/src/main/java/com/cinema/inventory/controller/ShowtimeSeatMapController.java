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

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeSeatMapController {

    private final ShowtimeSeatMapService seatMapService;

    public ShowtimeSeatMapController(
            ShowtimeSeatMapService seatMapService) {

        this.seatMapService = seatMapService;
    }

    @GetMapping("/{showtimeId}/seat-map")
    public ResponseEntity<ShowtimeSeatMapResponse> getShowtimeSeatMap(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(seatMapService.getSeatMap(showtimeId));
    }
}
