package com.cinema.booking.service;

import com.cinema.booking.entity.BookingTicket;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.*;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class BookingTicketService {
    private final BookingRepository bookings;
    private final BookingSeatRepository seats;
    private final BookingTicketRepository tickets;
    private final ProcessedEventRegistrationService processed;

    public BookingTicketService(
            BookingRepository bookings,
            BookingSeatRepository seats,
            BookingTicketRepository tickets,
            ProcessedEventRegistrationService processed) {
        this.bookings = bookings;
        this.seats = seats;
        this.tickets = tickets;
        this.processed = processed;
    }

    @Transactional
    public void issue(String key, OutboxEventMessage event) {
        if (event == null
                || event.eventId() == null
                || event.eventId().version() != 7
                || event.aggregateId() == null
                || event.aggregateId().version() != 7
                || event.occurredAt() == null
                || event.correlationId() == null
                || event.correlationId().version() != 7
                || event.causationId() == null
                || event.causationId().version() != 7
                || !event.aggregateId().toString().equals(key)
                || !"BOOKING".equals(event.aggregateType())
                || !"booking-service".equals(event.producer())
                || !"booking-confirmed".equals(event.eventType())
                || !"1".equals(event.eventVersion())
                || event.payload() == null
                || !event.payload().isObject()
                || !event.aggregateId()
                        .toString()
                        .equals(event.payload().path("bookingId").asText())) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
        if (!processed.register(
                event.eventId(),
                "booking-ticket-issuance",
                event.eventType(),
                event.eventVersion())) return;
        var booking =
                bookings.findByIdForUpdate(event.aggregateId())
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        if (!booking.getShowtimeId().toString().equals(event.payload().path("showtimeId").asText())
                || !booking.getUserId()
                        .toString()
                        .equals(event.payload().path("userId").asText())) {
            throw new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);
        }
        var existing = tickets.findAllByBookingIdOrderBySeatNumberAsc(booking.getId());
        var ids = new HashSet<UUID>();
        existing.forEach(t -> ids.add(t.getBookingSeatId()));
        var bookingSeats = seats.findAllByBookingIdOrderBySeatNumberAsc(booking.getId());
        if (bookingSeats.isEmpty())
            throw new ValidationException(BookingErrorCode.SEAT_NUMBERS_REQUIRED);
        for (var seat : bookingSeats)
            if (!ids.contains(seat.getId())) tickets.save(new BookingTicket(seat));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> read(UUID owner, UUID bookingId) {
        bookings.findByIdAndUserId(bookingId, owner)
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        return tickets.findAllByBookingIdOrderBySeatNumberAsc(bookingId).stream()
                .map(t ->
                        new TicketResponse(
                                t.getId(),
                                t.getBookingId(),
                                t.getBookingSeatId(),
                                t.getSeatNumber(),
                                t.getCapacity(),
                                t.getStatus(),
                                t.getQrPayload()))
                .toList();
    }

    public record TicketResponse(
            UUID id,
            UUID bookingId,
            UUID bookingSeatId,
            String seatNumber,
            int capacity,
            String status,
            String qrPayload) {}
}
