package com.cinema.inventory.dto.response;

import com.cinema.inventory.enums.SeatType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ShowtimeSeatQuoteResponse(
        UUID showtimeId,
        OffsetDateTime serverTime,
        String currency,
        int seatCount,
        int capacity,
        BigDecimal totalAmount,
        List<SeatQuote> seats) {

    public record SeatQuote(
            UUID showSeatId,
            String seatNumber,
            SeatType seatType,
            int capacity,
                    BigDecimal price) {}
}
