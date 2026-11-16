package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.common.response.model.PageResponse;

import java.util.UUID;
import java.util.List;

public interface BookingService {

    BookingResponse create(UUID userId, CreateBookingRequest request);

    BookingResponse findById(UUID userId, UUID bookingId);

    PageResponse<BookingResponse> findAll(UUID userId, int page, int size);

    PageResponse<BookingResponse> findAll(UUID userId, int page, int size, BookingStatus status);

    PageResponse<BookingResponse> findAllByStatuses(
            UUID userId, int page, int size, List<BookingStatus> statuses);
}
