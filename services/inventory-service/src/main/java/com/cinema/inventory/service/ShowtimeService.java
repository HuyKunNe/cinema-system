package com.cinema.inventory.service;

import com.cinema.common.response.model.PageResponse;
import com.cinema.inventory.dto.request.CreateShowtimeBatchRequest;
import com.cinema.inventory.dto.request.CreateShowtimeRequest;
import com.cinema.inventory.dto.request.UpdateShowtimeRequest;
import com.cinema.inventory.dto.response.ShowtimeResponse;
import com.cinema.inventory.enums.ShowtimeStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ShowtimeService {

    PageResponse<ShowtimeResponse> getManagedShowtimes(
            UUID cinemaId, UUID movieId, UUID roomId, ShowtimeStatus status,
            OffsetDateTime from, OffsetDateTime to, int page, int size);

    ShowtimeResponse create(CreateShowtimeRequest request);

    List<ShowtimeResponse> createBatch(CreateShowtimeBatchRequest request);

    ShowtimeResponse getById(UUID showtimeId);

    List<ShowtimeResponse> getByRoomId(UUID roomId);

    List<ShowtimeResponse> getByMovieId(UUID movieId);

    List<ShowtimeResponse> getByTimeRange(OffsetDateTime from, OffsetDateTime to);

    List<ShowtimeResponse> getBookableShowtimes(
            UUID cinemaId, UUID movieId, OffsetDateTime from, OffsetDateTime to);

    ShowtimeResponse update(UUID showtimeId, UpdateShowtimeRequest request);

    ShowtimeResponse openForBooking(UUID showtimeId);

    ShowtimeResponse openForBookingAutomatically(UUID showtimeId);

    ShowtimeResponse close(UUID showtimeId);

    void closeSalesAutomatically(UUID showtimeId);

    ShowtimeResponse cancel(UUID showtimeId);

    ShowtimeResponse complete(UUID showtimeId);
}
