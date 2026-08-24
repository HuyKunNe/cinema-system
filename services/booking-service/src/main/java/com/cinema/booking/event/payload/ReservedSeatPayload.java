package com.cinema.booking.event.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservedSeatPayload(
        UUID inventorySeatId, String seatNumber, String seatType, BigDecimal price) {}
