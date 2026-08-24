package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.common.outbox.entity.OutboxEventEntity;

public interface BookingCancelledOutboxFactory {

    OutboxEventEntity create(Booking booking, BookingCancellationReason reason);
}
