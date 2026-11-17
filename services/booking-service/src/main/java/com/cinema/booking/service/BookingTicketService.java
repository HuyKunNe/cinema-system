package com.cinema.booking.service;

import com.cinema.booking.entity.BookingTicket;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.validation.BookingTicketSnapshotValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;



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
        BookingTicketSnapshotValidator.validate(
                booking, bookingSeats, event);
        for (var seat : bookingSeats)
            if (!ids.contains(seat.getId())) tickets.save(new BookingTicket(seat));
    }

    @Transactional
    public RecoveryResult recover(UUID bookingId, UUID actor, boolean dryRun) {
        Objects.requireNonNull(actor);
        var booking =
                bookings.findByIdForUpdate(bookingId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        }
        var bookingSeats = seats.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        if (bookingSeats.isEmpty())
            throw new ValidationException(BookingErrorCode.SEAT_NUMBERS_REQUIRED);
        var existing = tickets.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        Set<UUID> issued = new HashSet<>();
        existing.forEach(ticket -> issued.add(ticket.getBookingSeatId()));
        int missing = 0;
        for (var seat : bookingSeats) {
            if (!seat.hasCompletedSnapshot()) {
                throw new ConflictException(BookingErrorCode.RESERVATION_RESULT_MISMATCH);
            }
            if (!issued.contains(seat.getId())) {
                BookingTicket.capacityFor(seat);
                missing++;
                if (!dryRun) tickets.save(new BookingTicket(seat, actor));
            }
        }
        return new RecoveryResult(
                bookingId,
                dryRun,
                bookingSeats.size(),
                existing.size(),
                dryRun ? 0 : missing,
                dryRun ? missing : 0);
    }

    public record RecoveryResult(
            UUID bookingId,
            boolean dryRun,
            int expectedTickets,
            int existingTickets,
            int createdTickets,
            int wouldCreateTickets) {}

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
                                t.getQrPayload(), t.getCheckedInAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LookupResponse lookup(String qrPayload) {
        String prefix = "cinema-ticket:v1:";
        if (qrPayload == null || !qrPayload.matches("^cinema-ticket:v1:[A-Za-z0-9_-]{43}$")) {
            throw new ValidationException(BookingErrorCode.TICKET_QR_INVALID);
        }
        String token = qrPayload.substring(prefix.length());
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            if (decoded.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(token)) {
                throw new ValidationException(BookingErrorCode.TICKET_QR_INVALID);
            }
        } catch (IllegalArgumentException failure) {
            throw new ValidationException(BookingErrorCode.TICKET_QR_INVALID);
        }
        var ticket = tickets.findByQrToken(token)
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.TICKET_NOT_FOUND));
        var booking = bookings.findById(ticket.getBookingId())
                .orElseThrow(() -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        return new LookupResponse(ticket.getId(), ticket.getBookingId(), booking.getShowtimeId(),
                ticket.getSeatNumber(), ticket.getCapacity(), ticket.getStatus(),
                booking.getStatus(), booking.getShowtimeEndsAt(), ticket.getCheckedInAt());
    }

    public record LookupResponse(UUID ticketId, UUID bookingId, UUID showtimeId,
            String seatNumber, int capacity, String ticketStatus, BookingStatus bookingStatus,
            OffsetDateTime showtimeEndsAt,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            OffsetDateTime checkedInAt) {
        public LookupResponse(UUID ticketId, UUID bookingId, UUID showtimeId,
                String seatNumber, int capacity, String ticketStatus, BookingStatus bookingStatus,
                OffsetDateTime showtimeEndsAt) {
            this(ticketId, bookingId, showtimeId, seatNumber, capacity, ticketStatus,
                    bookingStatus, showtimeEndsAt, null);
        }
    }

    public record TicketResponse(
            UUID id,
            UUID bookingId,
            UUID bookingSeatId,
            String seatNumber,
            int capacity,
            String status,
            String qrPayload,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            OffsetDateTime checkedInAt) {
        public TicketResponse(UUID id, UUID bookingId, UUID bookingSeatId, String seatNumber,
                int capacity, String status, String qrPayload) {
            this(id, bookingId, bookingSeatId, seatNumber, capacity, status, qrPayload, null);
        }

        @Override
        public String toString() {
            return "TicketResponse[id=" + id + ", status=" + status + ", qrPayload=REDACTED]";
        }
    }
}
