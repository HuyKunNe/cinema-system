package com.cinema.booking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingSeatResponse(
        UUID id,
        UUID inventorySeatId,
        UUID showtimeId,
        String seatNumber,
        String seatType,
        @Schema(
                        description =
                                "Price for one inventory seat. "
                                        + "For COUPLE, this is the entire pair price.")
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

    @JsonProperty("capacity")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            description =
                    "Number of people accommodated by this inventory seat. "
                            + "COUPLE accommodates two people. "
                            + "Null when the seat-type snapshot is unavailable or unsupported.")
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
    @Schema(
            nullable = true,
            requiredMode = Schema.RequiredMode.REQUIRED,
            accessMode = Schema.AccessMode.READ_ONLY,
            allowableValues = {"SEAT", "PAIR"},
            description =
                    "Pricing unit for the stored price. "
                            + "COUPLE uses PAIR; its price covers the entire pair. "
                            + "Null when the seat-type snapshot is unavailable or unsupported.")
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
