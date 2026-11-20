package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.client.PrintSimulationAccessClient.Access;
import com.cinema.booking.client.PrintSimulationAccessClient.Schedule;
import com.cinema.booking.dto.response.PrintSimulationResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingPrintSimulation;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.enums.PrintSimulationStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingPrintSimulationRepository;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ForbiddenException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PrintSimulationReadTest {

    private static final UUID BOOKING = UUID.fromString("019c0000-0000-7000-8000-000000000001");
    private static final UUID ACTOR = UUID.fromString("019c0000-0000-7000-8000-000000000002");
    private static final UUID SHOWTIME = UUID.fromString("019c0000-0000-7000-8000-000000000003");
    private static final UUID CINEMA = UUID.fromString("019c0000-0000-7000-8000-000000000004");
    private static final UUID OTHER_CINEMA =
            UUID.fromString("019c0000-0000-7000-8000-000000000005");
    private static final UUID READER = UUID.fromString("019c0000-0000-7000-8000-000000000006");
    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingSeatRepository seats = mock(BookingSeatRepository.class);
    private final BookingTicketRepository tickets = mock(BookingTicketRepository.class);
    private final BookingPrintSimulationRepository simulations =
            mock(BookingPrintSimulationRepository.class);
    private final ObjectMapper mapper = new JacksonConfiguration().objectMapper();
    private final Booking booking = mock(Booking.class);
    private final Access scope = new Access(READER, false, CINEMA);
    private final Schedule schedule =
            new Schedule(
                    SHOWTIME,
                    CINEMA,
                    OffsetDateTime.parse("2026-11-20T02:00:00Z"),
                    OffsetDateTime.parse("2026-11-20T04:00:00Z"),
                    "CLOSED");
    private final PrintSimulationTransactionService service =
            new PrintSimulationTransactionService(
                    bookings,
                    seats,
                    tickets,
                    simulations,
                    mapper,
                    Clock.fixed(Instant.parse("2026-11-20T03:00:00Z"), ZoneOffset.UTC));

    @Test
    void returnsOriginalSnapshotAfterCreationWindowWithoutSimulatingAgain()
            throws JsonProcessingException {
        confirmedBooking();
        var job = job(CINEMA);
        when(simulations.findByBookingId(BOOKING)).thenReturn(Optional.of(job));
        String stored = job.getResponseJson();
        var expected = mapper.readValue(stored, PrintSimulationResponse.class);

        var result = service.get(BOOKING, scope, schedule);

        assertThat(result).isEqualTo(expected);
        assertThat(result.actorId()).isEqualTo(ACTOR);
        assertThat(result.mode()).isEqualTo("SIMULATION");
        assertThat(job.getResponseJson()).isEqualTo(stored);
        verify(simulations, never()).saveAndFlush(any(BookingPrintSimulation.class));
        verifyNoInteractions(seats, tickets);
    }

    @Test
    void missingSimulationReturnsNotFoundInsteadOfCreatingJob() {
        confirmedBooking();
        when(simulations.findByBookingId(BOOKING)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(BOOKING, scope, schedule))
                .isInstanceOfSatisfying(
                        NotFoundException.class,
                        failure ->
                                assertThat(failure.getErrorCode())
                                        .isSameAs(BookingErrorCode.PRINT_SIMULATION_NOT_FOUND));
        verify(simulations, never()).saveAndFlush(any(BookingPrintSimulation.class));
        verifyNoInteractions(seats, tickets);
    }

    @Test
    void staffCannotReadSnapshotFromAnotherRecordedCinema() throws JsonProcessingException {
        confirmedBooking();
        when(simulations.findByBookingId(BOOKING)).thenReturn(Optional.of(job(OTHER_CINEMA)));

        assertThatThrownBy(() -> service.get(BOOKING, scope, schedule))
                .isInstanceOf(ForbiddenException.class);
        verify(simulations, never()).saveAndFlush(any(BookingPrintSimulation.class));
        verifyNoInteractions(seats, tickets);
    }

    @Test
    void staffCannotReadWhenCurrentCinemaIsOutsideAssignment() {
        confirmedBooking();

        assertThatThrownBy(() ->
                        service.get(BOOKING, new Access(READER, false, OTHER_CINEMA), schedule))
                .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(simulations, seats, tickets);
    }

    @Test
    void cancelledBookingRemainsBlockedBeforeReadingSnapshot() {
        confirmedBooking();
        when(booking.getStatus()).thenReturn(BookingStatus.CANCELLED);

        assertThatThrownBy(() -> service.get(BOOKING, scope, schedule))
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        failure ->
                                assertThat(failure.getErrorCode())
                                        .isSameAs(BookingErrorCode.BOOKING_NOT_CONFIRMED));
        verifyNoInteractions(simulations, seats, tickets);
    }

    private void confirmedBooking() {
        when(bookings.findByIdForUpdate(BOOKING)).thenReturn(Optional.of(booking));
        when(booking.getId()).thenReturn(BOOKING);
        when(booking.getShowtimeId()).thenReturn(SHOWTIME);
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
    }

    private BookingPrintSimulation job(UUID cinemaId) throws JsonProcessingException {
        var job =
                new BookingPrintSimulation(
                        BOOKING,
                        ACTOR,
                        cinemaId,
                        "unit-request",
                        "POS-UNIT",
                        PrintSimulationStatus.SIMULATED_SUCCEEDED);
        var response =
                new PrintSimulationResponse(
                        job.getId(),
                        BOOKING,
                        "000001",
                        "SIMULATION",
                        job.getStatus(),
                        ACTOR,
                        "POS-UNIT",
                        cinemaId,
                        SHOWTIME,
                        OffsetDateTime.parse("2026-11-20T01:30:00Z"),
                        List.of(
                                new PrintSimulationResponse.Ticket(
                                        UUID.fromString("019c0000-0000-7000-8000-000000000007"),
                                        UUID.fromString("019c0000-0000-7000-8000-000000000008"),
                                        "A1",
                                        1)),
                        1);
        job.complete(mapper.writeValueAsString(response));
        return job;
    }
}
