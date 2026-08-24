package com.cinema.booking.service;

import com.cinema.booking.dto.response.BookingResponse;

import java.util.UUID;

public interface BookingCancellationService {

    BookingResponse cancel(UUID userId, UUID bookingId);
}
