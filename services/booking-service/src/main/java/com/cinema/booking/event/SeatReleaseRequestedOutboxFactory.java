package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;

import java.time.OffsetDateTime;
import java.util.List;

public interface SeatReleaseRequestedOutboxFactory {

    OutboxEventEntity create(
            Booking booking,
            List<BookingSeat> seats,
            OutboxEventMessage sourceEvent,
            OffsetDateTime requestedAt);
}
