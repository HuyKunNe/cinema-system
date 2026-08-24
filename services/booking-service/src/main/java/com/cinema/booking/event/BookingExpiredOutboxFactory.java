package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.common.outbox.entity.OutboxEventEntity;

import java.time.OffsetDateTime;

public interface BookingExpiredOutboxFactory {

    OutboxEventEntity create(Booking booking, OffsetDateTime expiredAt);
}
