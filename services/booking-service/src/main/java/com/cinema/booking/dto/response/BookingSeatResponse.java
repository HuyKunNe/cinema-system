package com.cinema.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatResponse(
        UUID id,
        UUID inventorySeatId,
        UUID showtimeId,
        String seatNumber,
        String seatType,
        BigDecimal price,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "Requested ticket group. Null means the "
                                        + "customer did not request a specific group.")
                UUID requestedTicketGroupId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "Ticket group applied by Inventory. "
                                        + "Null before reservation or for legacy snapshots.")
                UUID ticketGroupId) {

    public BookingSeatResponse(
            UUID id,
            UUID inventorySeatId,
            UUID showtimeId,
            String seatNumber,
            String seatType,
            BigDecimal price) {

        this(id, inventorySeatId, showtimeId, seatNumber, seatType, price, null, null);
    }
}
