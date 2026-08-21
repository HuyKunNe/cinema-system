package com.cinema.inventory.event.payload;

import com.cinema.inventory.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservedSeatPayload(
        UUID inventorySeatId, String seatNumber, SeatType seatType, BigDecimal price) {}
