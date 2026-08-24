package com.cinema.booking.event.payload;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingCancelledPayload(
        UUID bookingId, UUID userId, UUID showtimeId, String reason, OffsetDateTime cancelledAt) {}
