package com.cinema.booking.service;

import com.cinema.booking.client.PrintSimulationAccessClient.Access;
import com.cinema.booking.client.PrintSimulationAccessClient.Schedule;
import com.cinema.booking.dto.request.PrintSimulationRequest;
import com.cinema.booking.dto.response.PrintSimulationResponse;
import com.cinema.booking.entity.BookingPrintSimulation;
import com.cinema.booking.entity.BookingTicket;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingPrintSimulationRepository;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.exception.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PrintSimulationTransactionService {

    private final BookingRepository bookings;
    private final BookingSeatRepository seats;
    private final BookingTicketRepository tickets;
    private final BookingPrintSimulationRepository simulations;
    private final ObjectMapper mapper;
    private final Clock clock;

    public PrintSimulationTransactionService(
            BookingRepository bookings,
            BookingSeatRepository seats,
            BookingTicketRepository tickets,
            BookingPrintSimulationRepository simulations,
            ObjectMapper mapper,
            @Qualifier("systemClock") Clock clock) {
        this.bookings = bookings;
        this.seats = seats;
        this.tickets = tickets;
        this.simulations = simulations;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public PrintSimulationResponse simulate(
            UUID bookingId,
            String key,
            PrintSimulationRequest request,
            Access access,
            Schedule schedule) {
        var booking =
                bookings.findByIdForUpdate(bookingId)
                        .orElseThrow(
                                () -> new NotFoundException(BookingErrorCode.BOOKING_NOT_FOUND));
        access.requireCinema(schedule.cinemaId());
        if (!booking.getShowtimeId().equals(schedule.id())
                || booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ConflictException(BookingErrorCode.BOOKING_NOT_CONFIRMED);
        }
        var existing = simulations.findByBookingId(bookingId);
        if (existing.isPresent()) {
            var job = existing.get();
            access.requireCinema(job.getCinemaId());
            if (!key.equals(job.getIdempotencyKey())) {
                throw new ConflictException(BookingErrorCode.PRINT_SIMULATION_ALREADY_EXISTS);
            }
            if (!access.userId().equals(job.getActorId())
                    || !request.deviceId().equals(job.getDeviceId())
                    || request.outcome() != job.getStatus()) {
                throw new ConflictException(BookingErrorCode.PRINT_IDEMPOTENCY_CONFLICT);
            }
            return read(job);
        }
        if (simulations.existsByActorIdAndIdempotencyKey(access.userId(), key)) {
            throw new ConflictException(BookingErrorCode.PRINT_IDEMPOTENCY_CONFLICT);
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (now.isAfter(schedule.startsAt().plusMinutes(15))) {
            throw new ConflictException(BookingErrorCode.PRINT_WINDOW_CLOSED);
        }
        var bookingSeats = seats.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        var issued = tickets.findAllByBookingIdOrderBySeatNumberAsc(bookingId);
        if (bookingSeats.isEmpty() || issued.size() != bookingSeats.size()) {
            throw new ConflictException(BookingErrorCode.PRINT_TICKETS_NOT_READY);
        }
        Set<UUID> matched = new HashSet<>();
        for (var ticket : issued) {
            if (!"ISSUED".equals(ticket.getStatus())) {
                throw new ConflictException(BookingErrorCode.PRINT_TICKET_NOT_ISSUED);
            }
            var seat =
                    bookingSeats.stream()
                            .filter(s -> s.getId().equals(ticket.getBookingSeatId()))
                            .findFirst()
                            .orElseThrow(() ->
                                    new ConflictException(
                                            BookingErrorCode.PRINT_TICKETS_NOT_READY));
            if (!seat.hasCompletedSnapshot()
                    || !bookingId.equals(seat.getBookingId())
                    || !bookingId.equals(ticket.getBookingId())
                    || !ticket.getSeatNumber().equals(seat.getSeatNumber())
                    || !matched.add(seat.getId())) {
                throw new ConflictException(BookingErrorCode.PRINT_TICKETS_NOT_READY);
            }
            if (!Set.of("COUPLE", "STANDARD", "VIP", "ACCESSIBLE").contains(seat.getSeatType())
                    || BookingTicket.capacityFor(seat) != ticket.getCapacity()) {
                throw new ConflictException(BookingErrorCode.PRINT_TICKETS_NOT_READY);
            }
        }
        var job =
                new BookingPrintSimulation(
                        bookingId,
                        access.userId(),
                        schedule.cinemaId(),
                        key,
                        request.deviceId(),
                        request.outcome());
        String id = bookingId.toString();
        var response =
                new PrintSimulationResponse(
                        job.getId(),
                        bookingId,
                        id.substring(id.length() - 6).toUpperCase(Locale.ROOT),
                        "SIMULATION",
                        request.outcome(),
                        access.userId(),
                        request.deviceId(),
                        schedule.cinemaId(),
                        schedule.id(),
                        now,
                        issued.stream()
                                .map(ticket ->
                                        new PrintSimulationResponse.Ticket(
                                                ticket.getId(),
                                                ticket.getBookingSeatId(),
                                                ticket.getSeatNumber(),
                                                ticket.getCapacity()))
                                .toList(),
                        issued.stream().mapToInt(ticket -> ticket.getCapacity()).sum());
        try {
            job.complete(mapper.writeValueAsString(response));
        } catch (JsonProcessingException failure) {
            throw new InternalServerException(
                    BookingErrorCode.PRINT_SIMULATION_STORAGE_FAILED, failure);
        }
        try {
            simulations.saveAndFlush(job);
        } catch (DataIntegrityViolationException failure) {
            throw new ConflictException(BookingErrorCode.PRINT_IDEMPOTENCY_CONFLICT);
        }
        return response;
    }

    private PrintSimulationResponse read(BookingPrintSimulation job) {
        try {
            return mapper.readValue(job.getResponseJson(), PrintSimulationResponse.class);
        } catch (JsonProcessingException failure) {
            throw new InternalServerException(
                    BookingErrorCode.PRINT_SIMULATION_STORAGE_FAILED, failure);
        }
    }
}
