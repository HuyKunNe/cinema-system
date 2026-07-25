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

import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;
import com.cinema.inventory.service.SeatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/seats")
public class SeatController {

  private final SeatService seatService;

  public SeatController(SeatService seatService) {
    this.seatService = seatService;
  }

  @PostMapping
  public ResponseEntity<SeatResponse> create(
      @RequestParam UUID roomId,
      @Valid @RequestBody CreateSeatRequest request) {

    SeatResponse response = seatService.create(roomId, request);

    return ResponseEntity
        .created(URI.create(
            "/api/v1/seats/" + response.id()))
        .body(response);
  }

  @GetMapping("/{seatId}")
  public ResponseEntity<SeatResponse> getById(
      @PathVariable("seatId") UUID seatId) {

    return ResponseEntity.ok(
        seatService.getById(seatId));
  }

  @GetMapping
  public ResponseEntity<List<SeatResponse>> getActiveSeats(
      @RequestParam UUID roomId) {

    return ResponseEntity.ok(
        seatService.getActiveSeats(roomId));
  }

  @PutMapping("/{seatId}")
  public ResponseEntity<SeatResponse> update(
      @PathVariable("seatId") UUID seatId,
      @Valid @RequestBody UpdateSeatRequest request) {

    return ResponseEntity.ok(
        seatService.update(seatId, request));
  }
}