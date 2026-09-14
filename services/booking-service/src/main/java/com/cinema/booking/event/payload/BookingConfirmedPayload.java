package com.cinema.booking.event.payload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingConfirmedPayload(
        UUID bookingId,
        UUID userId,
        UUID showtimeId,
        UUID paymentId,
        List<ConfirmedSeatPayload> seats,
        BigDecimal totalAmount,
        String currency,
        OffsetDateTime confirmedAt) {

    public BookingConfirmedPayload {

        seats = seats == null ? List.of() : List.copyOf(seats);
    }
}
