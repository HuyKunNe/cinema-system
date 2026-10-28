package com.cinema.inventory.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

public record RequestedSeatPayload(
        String seatNumber, @JsonInclude(JsonInclude.Include.NON_NULL) UUID ticketGroupId) {

    public RequestedSeatPayload(String seatNumber) {
        this(seatNumber, null);
    }
}

