package com.cinema.inventory.controller;

import com.cinema.inventory.dto.response.ShowtimeTicketPricesResponse;
import com.cinema.inventory.service.ShowtimeTicketPricesService;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
public class ShowtimeTicketPricesController {

    private final ShowtimeTicketPricesService service;

    public ShowtimeTicketPricesController(ShowtimeTicketPricesService service) {

        this.service = service;
    }

    @GetMapping("/{showtimeId}/ticket-prices")
    public ResponseEntity<ShowtimeTicketPricesResponse> getShowtimeTicketPrices(
            @PathVariable("showtimeId") UUID showtimeId) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.get(showtimeId));
    }
}
