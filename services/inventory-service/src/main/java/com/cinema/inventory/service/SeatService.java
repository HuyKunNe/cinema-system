package com.cinema.inventory.service;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.CreateSeatRangeRequest;
import com.cinema.inventory.dto.request.CreateSeatRequest;
import com.cinema.inventory.dto.request.UpdateSeatActiveRequest;
import com.cinema.inventory.dto.request.UpdateSeatRequest;
import com.cinema.inventory.dto.response.SeatActiveStateResponse;
import com.cinema.inventory.dto.response.SeatResponse;

public interface SeatService {

    SeatResponse create(
            UUID roomId,
            CreateSeatRequest request);

    List<SeatResponse> createRange(
            UUID roomId,
            CreateSeatRangeRequest request);

    SeatResponse getById(UUID seatId);

    List<SeatResponse> getActiveSeats(UUID roomId);

    List<SeatResponse> getManagedSeats(
            UUID roomId,
            Boolean active);

    SeatResponse update(
            UUID seatId,
            UpdateSeatRequest request);

    SeatActiveStateResponse updateActive(
            UUID seatId,
            UpdateSeatActiveRequest request);
}
