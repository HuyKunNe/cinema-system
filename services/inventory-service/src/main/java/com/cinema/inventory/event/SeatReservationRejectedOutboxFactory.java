package com.cinema.inventory.event;

import com.cinema.common.outbox.entity.OutboxEventEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SeatReservationRejectedOutboxFactory {

    OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            SeatReservationRejectionReason reason,
            String message,
            List<String> unavailableSeats,
            OffsetDateTime rejectedAt,
            UUID correlationId,
            UUID causationId);
}
