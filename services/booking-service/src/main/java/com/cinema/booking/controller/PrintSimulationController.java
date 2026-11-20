package com.cinema.booking.controller;

import com.cinema.booking.dto.request.PrintSimulationRequest;
import com.cinema.booking.dto.response.PrintSimulationResponse;
import com.cinema.booking.service.PrintSimulationService;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.validation.annotation.UuidV7;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/print-simulations")
@ConditionalOnProperty(name = "cinema.booking.print-simulator.enabled", havingValue = "true")
public class PrintSimulationController {

    private final PrintSimulationService service;

    public PrintSimulationController(PrintSimulationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PrintSimulationResponse>> getBookingPrintSimulation(
            @PathVariable @UuidV7 UUID bookingId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.get(bookingId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PrintSimulationResponse>> simulateBookingPrint(
            @PathVariable @UuidV7 UUID bookingId,
            @RequestHeader(value = "Idempotency-Key", required = false)
                    @NotBlank
                    @Pattern(regexp = "[A-Za-z0-9._-]{1,100}")
                    String key,
            @Valid @RequestBody PrintSimulationRequest request) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseFactory.success(service.simulate(bookingId, key, request)));
    }
}
