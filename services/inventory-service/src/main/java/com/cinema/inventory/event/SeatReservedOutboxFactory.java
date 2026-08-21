package com.cinema.inventory.event;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.inventory.entity.ShowSeat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SeatReservedOutboxFactory {

    OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            List<ShowSeat> showSeats,
            OffsetDateTime heldAt,
            OffsetDateTime holdExpiresAt,
            UUID correlationId,
            UUID causationId);
}
