package com.cinema.inventory.service;

import com.cinema.inventory.dto.request.CreateSeatRangeRequest;
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatResponse;

import java.util.List;
import java.util.UUID;

public interface SeatService {

    SeatResponse create(UUID roomId, CreateSeatRequest request);

    List<SeatResponse> createRange(UUID roomId, CreateSeatRangeRequest request);

    SeatResponse getById(UUID seatId);

    List<SeatResponse> getActiveSeats(UUID roomId);

    SeatResponse update(UUID seatId, UpdateSeatRequest request);
}
