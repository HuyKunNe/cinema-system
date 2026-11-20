package com.cinema.booking.dto.response;

import com.cinema.booking.enums.PrintSimulationStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PrintSimulationResponse(
        UUID printJobId,
        UUID bookingId,
        String displayCode,
        String mode,
        PrintSimulationStatus status,
        UUID actorId,
        String deviceId,
        UUID cinemaId,
        UUID showtimeId,
        OffsetDateTime completedAt,
        List<Ticket> tickets,
        int totalGuests) {

    public record Ticket(UUID ticketId, UUID bookingSeatId, String seatNumber, int capacity) {}
}
