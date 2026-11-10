package com.cinema.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
                UUID requestedTicketGroupId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
                UUID ticketGroupId,
        @JsonInclude(JsonInclude.Include.ALWAYS)
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

    @JsonProperty("capacity")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public Integer getCapacity() {

        if (seatType == null) {
            return null;
        }

        return switch (seatType) {
            case "STANDARD", "VIP", "ACCESSIBLE" -> 1;
            case "COUPLE" -> 2;
            default -> null;
        };
    }

    @JsonProperty("unit")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public String getUnit() {

        if (seatType == null) {
            return null;
        }

        return switch (seatType) {
            case "STANDARD", "VIP", "ACCESSIBLE" -> "SEAT";
            case "COUPLE" -> "PAIR";
            default -> null;
        };
    }
}
