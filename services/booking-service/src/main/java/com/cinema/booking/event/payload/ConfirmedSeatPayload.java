package com.cinema.booking.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.UUID;

public record ConfirmedSeatPayload(
        String seatNumber,
        String seatType,
        BigDecimal price,
        @JsonInclude(JsonInclude.Include.NON_NULL)
                UUID ticketGroupId) {

    public ConfirmedSeatPayload(
            String seatNumber,
            String seatType,
            BigDecimal price) {

        this(seatNumber, seatType, price, null);
    }
}
