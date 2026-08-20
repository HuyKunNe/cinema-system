package com.cinema.booking.service;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.common.response.model.PageResponse;

import java.util.UUID;

public interface BookingService {

    BookingResponse create(UUID userId, CreateBookingRequest request);

    BookingResponse findById(UUID userId, UUID bookingId);

    PageResponse<BookingResponse> findAll(UUID userId, int page, int size);
}
