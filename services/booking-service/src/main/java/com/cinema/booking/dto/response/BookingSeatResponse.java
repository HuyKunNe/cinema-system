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
                @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
                UUID requestedTicketGroupId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED)
                UUID ticketGroupId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                @Schema(
                        nullable = true,
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        description =
                                "Ticket-group name captured during reservation. "
                                        + "Null before reservation or for legacy snapshots.")
                String ticketGroupName) {

    public BookingSeatResponse(
            UUID id,
            UUID inventorySeatId,
            UUID showtimeId,
            String seatNumber,
            String seatType,
            BigDecimal price) {

        this(id, inventorySeatId, showtimeId, seatNumber, seatType, price, null, null, null);
    }

    public BookingSeatResponse(
            UUID id,
            UUID inventorySeatId,
            UUID showtimeId,
            String seatNumber,
            String seatType,
            BigDecimal price,
            UUID requestedTicketGroupId,
            UUID ticketGroupId) {

        this(
                id,
                inventorySeatId,
                showtimeId,
                seatNumber,
                seatType,
                price,
                requestedTicketGroupId,
                ticketGroupId,
                null);
    }
}
