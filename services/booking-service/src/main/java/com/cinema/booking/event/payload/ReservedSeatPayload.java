package com.cinema.booking.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

public record ReservedSeatPayload(
        UUID inventorySeatId,
        String seatNumber,
        String seatType,
        BigDecimal price,
        @JsonInclude(JsonInclude.Include.NON_NULL) UUID ticketGroupId,
        @JsonInclude(JsonInclude.Include.NON_NULL) String ticketGroupName) {

    public ReservedSeatPayload(
            UUID inventorySeatId, String seatNumber, String seatType, BigDecimal price) {

        this(inventorySeatId, seatNumber, seatType, price, null, null);
    }

    public ReservedSeatPayload(
            UUID inventorySeatId,
            String seatNumber,
            String seatType,
            BigDecimal price,
            UUID ticketGroupId) {

        this(inventorySeatId, seatNumber, seatType, price, ticketGroupId, null);
    }
}

