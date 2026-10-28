package com.cinema.inventory.event.payload;

import com.cinema.inventory.enums.SeatType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservedSeatPayload(
        UUID inventorySeatId,
        String seatNumber,
        SeatType seatType,
        BigDecimal price,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID ticketGroupId) {

    public ReservedSeatPayload(
            UUID inventorySeatId, String seatNumber, SeatType seatType, BigDecimal price) {

        this(inventorySeatId, seatNumber, seatType, price, null);
    }
}
