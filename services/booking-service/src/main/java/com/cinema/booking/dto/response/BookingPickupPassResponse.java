package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingPickupStatus;
import com.cinema.booking.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record BookingPickupPassResponse(
        UUID bookingId,
        String displayCode,
        BookingStatus bookingStatus,
        BookingPickupStatus status,
        int expectedTickets,
        int issuedTickets,
        OffsetDateTime expiresAt,
        String qrPayload) {

    @Override
    public String toString() {
        return "BookingPickupPassResponse[bookingId="
                + bookingId
                + ", status="
                + status
                + ", qrPayload=REDACTED]";
    }
}
