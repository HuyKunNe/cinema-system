package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID showtimeId,
        String clientRequestId,
        BookingStatus status,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancelledAt,
        String rejectionReason,
        List<BookingSeatResponse> seats,
        Long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
