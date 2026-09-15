package com.cinema.inventory.event.payload;

import java.math.BigDecimal;

public record ConfirmedSeatPayload(String seatNumber, String seatType, BigDecimal price) {}
