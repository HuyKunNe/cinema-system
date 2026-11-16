package com.cinema.booking.controller;

import com.cinema.booking.service.ShowtimeEndBackfillService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.security.authentication.CurrentUser;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/management/showtime-end-backfill")
public class ShowtimeEndBackfillController {
    private final ShowtimeEndBackfillService service;

    public ShowtimeEndBackfillController(ShowtimeEndBackfillService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShowtimeEndBackfillService.Batch>> backfillShowtimeEnds(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int size,
            @RequestParam(required = false) @UuidV7 UUID afterId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        ResponseFactory.success(
                                service.run(afterId, size, dryRun, CurrentUser.id())));
    }
}
