package com.cinema.booking.service;

import com.cinema.booking.client.PrintSimulationAccessClient.Access;
import com.cinema.booking.client.PrintSimulationAccessClient.PickupSchedule;
import com.cinema.booking.client.PrintSimulationAccessClient.Schedule;
import com.cinema.booking.dto.response.BookingPickupLookupResponse;
import com.cinema.booking.dto.response.BookingPickupPassResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingPickupPass;
import com.cinema.booking.enums.BookingPickupStatus;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingPickupPassRepository;
import com.cinema.booking.repository.BookingPrintSimulationRepository;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.booking.validation.BookingTicketSetValidator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ServiceUnavailableException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingPickupTransactionService {

    private final BookingRepository bookings;
    private final BookingSeatRepository seats;
    private final BookingTicketRepository tickets;
    private final BookingPickupPassRepository passes;
    private final BookingPrintSimulationRepository simulations;
    private final BookingPickupTokenCodec tokens;
    private final Clock clock;
    private final boolean simulationEnabled;

    public BookingPickupTransactionService(
            BookingRepository bookings,
            BookingSeatRepository seats,
            BookingTicketRepository tickets,
            BookingPickupPassRepository passes,
            BookingPrintSimulationRepository simulations,
            BookingPickupTokenCodec tokens,
            @Qualifier("systemClock") Clock clock,
            @Value("${cinema.booking.print-simulator.enabled:false}") boolean simulationEnabled) {
        this.bookings = bookings;
        this.seats = seats;
        this.tickets = tickets;
        this.passes = passes;
        this.simulations = simulations;
        this.tokens = tokens;
        this.clock = clock;
        this.simulationEnabled = simulationEnabled;
    }

    @Transactional
    public BookingPickupPassResponse owner(UUID owner, UUID bookingId, PickupSchedule context) {
        var booking =
                bookings.findByIdAndUserIdForUpdate(bookingId, owner)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        var bookingSeats = seats.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        var issued = tickets.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            return response(
                    booking,
                    BookingPickupStatus.NOT_CONFIRMED,
                    bookingSeats.size(),
                    issued.size(),
                    null,
                    null);
        }
        if (context == null) {
            // Local issuance state is known even when the schedule cannot be read.
            // No expiry or credential may be inferred without a verified context.
            BookingPickupStatus state =
                    !bookingSeats.isEmpty() && issued.size() < bookingSeats.size()
                            ? BookingPickupStatus.WAITING_FOR_TICKETS
                            : BookingPickupStatus.UNAVAILABLE;
            return response(booking, state, bookingSeats.size(), issued.size(), null, null);
        }
        if (!booking.getShowtimeId().equals(context.schedule().id())) {
            return response(
                    booking,
                    BookingPickupStatus.UNAVAILABLE,
                    bookingSeats.size(),
                    issued.size(),
                    null,
                    null);
        }
        var existing = passes.findByBookingId(bookingId);
        OffsetDateTime expiresAt = expiry(existing.orElse(null), context.schedule());
        BookingPickupStatus status;
        if (OffsetDateTime.now(clock).isAfter(expiresAt)) {
            status = BookingPickupStatus.EXPIRED;
        } else if (!usable(context)) {
            status = BookingPickupStatus.UNAVAILABLE;
        } else if (!bookingSeats.isEmpty() && issued.size() < bookingSeats.size()) {
            status = BookingPickupStatus.WAITING_FOR_TICKETS;
        } else if (!BookingTicketSetValidator.isComplete(booking, bookingSeats, issued)) {
            status = BookingPickupStatus.UNAVAILABLE;
        } else if (issued.stream().anyMatch(ticket -> "USED".equals(ticket.getStatus()))) {
            status = BookingPickupStatus.TICKETS_USED;
        } else if (issued.stream().anyMatch(ticket -> !"ISSUED".equals(ticket.getStatus()))) {
            status = BookingPickupStatus.UNAVAILABLE;
        } else {
            status = BookingPickupStatus.READY;
        }
        if (status != BookingPickupStatus.READY) {
            return response(booking, status, bookingSeats.size(), issued.size(), expiresAt, null);
        }
        BookingPickupPass pass = existing.orElse(null);
        if (pass == null) {
            pass = new BookingPickupPass(bookingId, tokens.generate(), expiresAt);
            try {
                passes.saveAndFlush(pass);
            } catch (DataIntegrityViolationException failure) {
                // Do not attach a SQL error that could contain the credential value.
                throw new InternalServerException(BookingErrorCode.PICKUP_PASS_ISSUE_FAILED);
            }
        }
        return response(
                booking,
                status,
                bookingSeats.size(),
                issued.size(),
                expiresAt,
                pass.getQrPayload());
    }

    @Transactional
    public BookingPickupLookupResponse lookup(
            UUID bookingId, UUID passId, Access access, PickupSchedule context) {
        var booking =
                bookings.findByIdForUpdate(bookingId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        Schedule schedule = context.schedule();
        if (!Boolean.TRUE.equals(access.administrator())
                && !schedule.cinemaId().equals(access.cinemaId())) {
            throw new ForbiddenException(BookingErrorCode.PICKUP_ACCESS_DENIED);
        }
        if (!booking.getShowtimeId().equals(schedule.id())
                || booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        }
        var pass =
                passes.findByBookingId(bookingId)
                        .filter(candidate -> candidate.getId().equals(passId))
                        .orElseThrow(() ->
                                new NotFoundException(BookingErrorCode.PICKUP_PASS_NOT_FOUND));
        OffsetDateTime expiresAt = expiry(pass, schedule);
        if (OffsetDateTime.now(clock).isAfter(expiresAt)) {
            throw new ConflictException(BookingErrorCode.PICKUP_PASS_EXPIRED);
        }
        if (!usable(context)) {
            throw new ConflictException(BookingErrorCode.PICKUP_NOT_AVAILABLE);
        }
        if (schedule.roomId() == null
                || schedule.movieId() == null
                || schedule.roomName() == null
                || schedule.cinemaName() == null) {
            throw new ServiceUnavailableException(BookingErrorCode.PICKUP_CONTEXT_UNAVAILABLE);
        }
        var bookingSeats = seats.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        var issued = tickets.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        boolean complete = BookingTicketSetValidator.isComplete(booking, bookingSeats, issued);
        boolean allIssued =
                complete && issued.stream().allMatch(ticket -> "ISSUED".equals(ticket.getStatus()));
        String blocked =
                !simulationEnabled
                        ? "SIMULATION_DISABLED"
                        : !complete
                                ? "TICKETS_NOT_READY"
                                : !allIssued
                                        ? "TICKET_NOT_ISSUED"
                                        : simulations.findByBookingId(bookingId).isPresent()
                                                ? "SIMULATION_ALREADY_EXISTS"
                                                : null;
        return new BookingPickupLookupResponse(
                bookingId,
                displayCode(bookingId),
                booking.getStatus(),
                expiresAt,
                new BookingPickupLookupResponse.Showtime(
                        schedule.id(),
                        schedule.movieId(),
                        schedule.cinemaId(),
                        schedule.cinemaName(),
                        schedule.roomId(),
                        schedule.roomName(),
                        schedule.startsAt(),
                        schedule.endsAt(),
                        schedule.status()),
                bookingSeats.size(),
                issued.size(),
                complete,
                complete ? issued.stream().mapToInt(ticket -> ticket.getCapacity()).sum() : null,
                issued.stream()
                        .map(ticket ->
                                new BookingPickupLookupResponse.Ticket(
                                        ticket.getId(),
                                        ticket.getBookingSeatId(),
                                        ticket.getSeatNumber(),
                                        ticket.getCapacity(),
                                        ticket.getStatus(),
                                        ticket.getCheckedInAt()))
                        .toList(),
                new BookingPickupLookupResponse.Printing(
                        false,
                        "PHYSICAL_PRINTING_NOT_SUPPORTED",
                        simulationEnabled,
                        blocked == null,
                        blocked));
    }

    private static boolean usable(PickupSchedule context) {
        return context.cinemaActive()
                && Set.of("SCHEDULED", "OPEN_FOR_BOOKING", "CLOSED")
                        .contains(context.schedule().status());
    }

    private static OffsetDateTime expiry(BookingPickupPass pass, Schedule schedule) {
        OffsetDateTime current = schedule.startsAt().plusMinutes(15);
        return pass != null && pass.getExpiresAt().isBefore(current)
                ? pass.getExpiresAt()
                : current;
    }

    private static String displayCode(UUID bookingId) {
        String id = bookingId.toString();
        return id.substring(id.length() - 6).toUpperCase(Locale.ROOT);
    }

    private static BookingPickupPassResponse response(
            Booking booking,
            BookingPickupStatus status,
            int expected,
            int issued,
            OffsetDateTime expiresAt,
            String payload) {
        return new BookingPickupPassResponse(
                booking.getId(),
                displayCode(booking.getId()),
                booking.getStatus(),
                status,
                expected,
                issued,
                expiresAt,
                payload);
    }
}
