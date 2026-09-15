package com.cinema.inventory.event;

import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.inventory.entity.ShowSeat;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SeatReleasedOutboxFactory {

    OutboxEventEntity create(
            UUID bookingId,
            UUID showtimeId,
            List<ShowSeat> releasedSeats,
            String reason,
            OffsetDateTime releasedAt,
            UUID correlationId,
            UUID causationId);
}
