package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;

import java.time.OffsetDateTime;

public interface PaymentRequestedOutboxFactory {

    OutboxEventEntity create(
            Booking booking, OutboxEventMessage sourceEvent, OffsetDateTime requestedAt);
}
