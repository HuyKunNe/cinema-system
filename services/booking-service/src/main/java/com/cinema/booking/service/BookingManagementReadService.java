package com.cinema.booking.service;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.common.response.model.PageResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface BookingManagementReadService {

    PageResponse<BookingResponse> findAll(
            BookingStatus status,
            UUID showtimeId,
            UUID userId,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size);

    BookingResponse findById(UUID bookingId);
}
