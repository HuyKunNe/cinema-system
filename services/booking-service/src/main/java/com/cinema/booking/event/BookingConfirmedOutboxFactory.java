package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;

import java.util.List;
import java.util.UUID;

public interface BookingConfirmedOutboxFactory {

    OutboxEventEntity create(
            Booking booking,
            List<BookingSeat> seats,
            UUID paymentId,
            OutboxEventMessage sourceEvent);
}
