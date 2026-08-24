package com.cinema.booking.event.payload;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingExpiredPayload(
        UUID bookingId, UUID userId, UUID showtimeId, OffsetDateTime expiredAt) {}
