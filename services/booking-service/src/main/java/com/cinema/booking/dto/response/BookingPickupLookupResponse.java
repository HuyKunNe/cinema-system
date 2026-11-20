package com.cinema.booking.dto.response;

import com.cinema.booking.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record BookingPickupLookupResponse(
        UUID bookingId,
        String displayCode,
        BookingStatus bookingStatus,
        OffsetDateTime expiresAt,
        Showtime showtime,
        int expectedTickets,
        int issuedTickets,
        boolean ticketsReady,
        Integer totalGuests,
        List<Ticket> tickets,
        Printing printing) {

    public record Showtime(
            UUID showtimeId,
            UUID movieId,
            UUID cinemaId,
            String cinemaName,
            UUID roomId,
            String roomName,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String status) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Ticket(
            UUID ticketId,
            UUID bookingSeatId,
            String seatNumber,
            int capacity,
            String status,
            OffsetDateTime checkedInAt) {}

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Printing(
            boolean canPrint,
            String printBlockReason,
            boolean simulationEnabled,
            boolean canSimulatePrint,
            String simulationBlockReason) {}
}
