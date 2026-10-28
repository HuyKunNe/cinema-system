package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        description = "Current seat-price snapshot before booking.",
        requiredProperties = {
            "showtimeId",
            "serverTime",
            "currency",
            "seatCount",
            "capacity",
            "totalAmount",
            "seats"
        })
public record ShowtimeSeatQuoteResponse(
        UUID showtimeId,
        OffsetDateTime serverTime,
        String currency,
        int seatCount,
        int capacity,
        BigDecimal totalAmount,
        List<SeatQuote> seats) {

    @Schema(requiredProperties = {"showSeatId", "seatNumber", "seatType", "capacity", "price"})
    public record SeatQuote(
            UUID showSeatId,
            String seatNumber,
            SeatType seatType,
            int capacity,
            @Schema(
                            description =
                                    "Price for one inventory seat. "
                                            + "For COUPLE, this is the entire pair price.")
                    BigDecimal price) {}
}
