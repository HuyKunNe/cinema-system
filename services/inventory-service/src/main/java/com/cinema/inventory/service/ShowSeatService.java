package com.cinema.inventory.service;

import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.GenerateShowSeatsRequest;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;

public interface ShowSeatService {

    List<ShowSeatResponse> generate(
            UUID showtimeId,
            GenerateShowSeatsRequest request);

    ShowSeatResponse getById(UUID showSeatId);

    List<ShowSeatResponse> getByShowtimeId(UUID showtimeId);

    List<ShowSeatResponse> getAvailableByShowtimeId(UUID showtimeId);

    ShowSeatResponse hold(UUID showSeatId, HoldShowSeatRequest request);

    ShowSeatResponse book(UUID showSeatId, ShowSeatBookingRequest request);

    ShowSeatResponse release(UUID showSeatId, ShowSeatBookingRequest request);
}
