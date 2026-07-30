package com.cinema.inventory.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.service.ShowSeatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/show-seats")
public class ShowSeatController {

  private final ShowSeatService showSeatService;

  public ShowSeatController(
      ShowSeatService showSeatService) {
    this.showSeatService = showSeatService;
  }

  @PostMapping
  public ResponseEntity<List<ShowSeatResponse>> generate(
      @RequestParam("showtimeId") UUID showtimeId,
      @Valid @RequestBody GenerateShowSeatsRequest request) {

    List<ShowSeatResponse> response = showSeatService.generate(showtimeId, request);

    return ResponseEntity
        .created(URI.create(
            "/api/v1/show-seats?showtimeId="
                + showtimeId))
        .body(response);
  }

  @GetMapping("/{showSeatId}")
  public ResponseEntity<ShowSeatResponse> getById(
      @PathVariable("showSeatId") UUID showSeatId) {

    return ResponseEntity.ok(
        showSeatService.getById(showSeatId));
  }

  @GetMapping
  public ResponseEntity<List<ShowSeatResponse>> getByShowtimeId(
      @RequestParam("showtimeId") UUID showtimeId,
      @RequestParam(name = "availableOnly", defaultValue = "false") boolean availableOnly) {

    if (availableOnly) {
      return ResponseEntity.ok(
          showSeatService
              .getAvailableByShowtimeId(showtimeId));
    }

    return ResponseEntity.ok(
        showSeatService.getByShowtimeId(showtimeId));
  }
}
