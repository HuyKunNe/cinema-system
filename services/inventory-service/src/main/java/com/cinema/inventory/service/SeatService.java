package com.cinema.inventory.service;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;

public interface SeatService {

  SeatResponse create(UUID roomId, CreateSeatRequest request);

  SeatResponse getById(UUID seatId);

  List<SeatResponse> getActiveSeats(UUID roomId);

  SeatResponse update(UUID seatId, UpdateSeatRequest request);
}