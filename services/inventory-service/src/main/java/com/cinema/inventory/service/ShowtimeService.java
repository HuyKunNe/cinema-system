package com.cinema.inventory.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;

public interface ShowtimeService {

  ShowtimeResponse create(CreateShowtimeRequest request);

  ShowtimeResponse getById(UUID showtimeId);

  List<ShowtimeResponse> getByRoomId(UUID roomId);

  List<ShowtimeResponse> getByMovieId(UUID movieId);

  List<ShowtimeResponse> getByTimeRange(
      OffsetDateTime from,
      OffsetDateTime to);

  ShowtimeResponse update(
      UUID showtimeId,
      UpdateShowtimeRequest request);

  ShowtimeResponse openForBooking(UUID showtimeId);

  ShowtimeResponse close(UUID showtimeId);

  ShowtimeResponse cancel(UUID showtimeId);

  ShowtimeResponse complete(UUID showtimeId);
}