package com.cinema.booking.service;

import com.cinema.booking.dto.response.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingCreationService {

    BookingResponse createNew(
            UUID userId,
            UUID showtimeId,
            String clientRequestId,
            String requestFingerprint,
            List<String> normalizedSeatNumbers);
}
