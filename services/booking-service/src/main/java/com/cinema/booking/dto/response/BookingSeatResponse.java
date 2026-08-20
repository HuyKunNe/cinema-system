package com.cinema.booking.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatResponse(
        UUID id,
        UUID inventorySeatId,
        UUID showtimeId,
        String seatNumber,
        String seatType,
        BigDecimal price) {}
