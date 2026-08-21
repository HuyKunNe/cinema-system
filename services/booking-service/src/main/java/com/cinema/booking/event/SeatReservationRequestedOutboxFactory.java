package com.cinema.booking.event;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.common.outbox.entity.OutboxEventEntity;

import java.time.OffsetDateTime;
import java.util.List;

public interface SeatReservationRequestedOutboxFactory {

    OutboxEventEntity create(
            Booking booking, List<BookingSeat> bookingSeats, OffsetDateTime occurredAt);
}
