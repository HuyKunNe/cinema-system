package com.cinema.inventory.service;

import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;

import java.util.List;
import java.util.UUID;

public interface ShowSeatService {
    ShowSeatResponse getById(UUID showSeatId);

    List<ShowSeatResponse> getByShowtimeId(UUID showtimeId);

    List<ShowSeatResponse> getAvailableByShowtimeId(UUID showtimeId);

    ShowSeatResponse hold(UUID showSeatId, HoldShowSeatRequest request);

    ShowSeatResponse book(UUID showSeatId, ShowSeatBookingRequest request);

    ShowSeatResponse release(UUID showSeatId, ShowSeatBookingRequest request);

    ShowSeatResponse makeUnavailable(UUID showSeatId);

    ShowSeatResponse makeAvailable(UUID showSeatId);
}
