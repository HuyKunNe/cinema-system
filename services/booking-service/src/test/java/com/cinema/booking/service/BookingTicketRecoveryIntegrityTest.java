package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingTicket;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class BookingTicketRecoveryIntegrityTest {

    private static final UUID BOOKING = UUID.fromString("019c0000-0000-7000-8000-000000000001");
    private static final UUID SHOWTIME = UUID.fromString("019c0000-0000-7000-8000-000000000002");
    private static final UUID ACTOR = UUID.fromString("019c0000-0000-7000-8000-000000000003");
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-26T10:00:00Z");

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingSeatRepository seats = mock(BookingSeatRepository.class);
    private final BookingTicketRepository tickets = mock(BookingTicketRepository.class);
    private final ProcessedEventRegistrationService processed =
            mock(ProcessedEventRegistrationService.class);
    private final Booking booking = mock(Booking.class);
    private final BookingSeat couple = seat(SHOWTIME, "A1", "COUPLE");
    private final BookingSeat single = seat(SHOWTIME, "A2", "STANDARD");
    private final BookingTicketService service =
            new BookingTicketService(bookings, seats, tickets, processed);

    @BeforeEach
    void prepare() {
        when(booking.getId()).thenReturn(BOOKING);
        when(booking.getShowtimeId()).thenReturn(SHOWTIME);
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(bookings.findByIdForUpdate(BOOKING)).thenReturn(Optional.of(booking));
        when(seats.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(couple, single));
    }

    @Test
    void dryRunCountsOnlyMissingTicketsWithoutChangingExistingCredential() {
        var existing = new BookingTicket(couple);
        String qr = existing.getQrPayload();
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING)).thenReturn(List.of(existing));

        var result = service.recover(BOOKING, ACTOR, true);

        assertThat(result.expectedTickets()).isEqualTo(2);
        assertThat(result.existingTickets()).isEqualTo(1);
        assertThat(result.wouldCreateTickets()).isEqualTo(1);
        assertThat(result.createdTickets()).isZero();
        assertThat(existing.getQrPayload()).isEqualTo(qr);
        verify(tickets, never()).save(any(BookingTicket.class));
    }

    @Test
    void recoveryCreatesOnlyMissingTicketAndPreservesUsedCoupleTicket() {
        var existing = new BookingTicket(couple);
        existing.checkIn(NOW, ACTOR, NOW);
        String qr = existing.getQrPayload();
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING)).thenReturn(List.of(existing));

        var result = service.recover(BOOKING, ACTOR, false);

        var saved = ArgumentCaptor.forClass(BookingTicket.class);
        verify(tickets).save(saved.capture());
        assertThat(saved.getValue().getBookingSeatId()).isEqualTo(single.getId());
        assertThat(saved.getValue().getCapacity()).isEqualTo(1);
        assertThat(result.createdTickets()).isEqualTo(1);
        assertThat(existing.getStatus()).isEqualTo("USED");
        assertThat(existing.getCapacity()).isEqualTo(2);
        assertThat(existing.getCheckedInAt()).isEqualTo(NOW);
        assertThat(existing.getQrPayload()).isEqualTo(qr);
    }

    @Test
    void wrongExistingCapacityBlocksRecoveryBeforeCreatingAnyTicket() {
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(wrongCapacity()));

        assertMismatch(() -> service.recover(BOOKING, ACTOR, false));
    }

    @Test
    void dryRunAlsoRejectsWrongExistingCapacity() {
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(wrongCapacity()));

        assertMismatch(() -> service.recover(BOOKING, ACTOR, true));
    }

    @Test
    void unissuedSeatFromAnotherShowtimeBlocksAllRecoveryWrites() {
        when(seats.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(couple, seat(ACTOR, "A2", "STANDARD")));
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING)).thenReturn(List.of());

        assertMismatch(() -> service.recover(BOOKING, ACTOR, false));
    }

    @Test
    void repeatedExistingSeatDoesNotMasqueradeAsCompleteIssuance() {
        var existing = new BookingTicket(couple);
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(existing, existing));

        assertMismatch(() -> service.recover(BOOKING, ACTOR, false));
    }

    @Test
    void validConfirmationEventCannotFillAroundAnInconsistentExistingTicket() {
        when(booking.getUserId()).thenReturn(ACTOR);
        when(booking.getTotalAmount()).thenReturn(new BigDecimal("100000.00"));
        when(booking.getCurrency()).thenReturn("VND");
        when(booking.getConfirmedAt()).thenReturn(NOW);
        when(processed.register(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING))
                .thenReturn(List.of(wrongCapacity()));
        var payload = JsonNodeFactory.instance.objectNode();
        payload.put("bookingId", BOOKING.toString());
        payload.put("showtimeId", SHOWTIME.toString());
        payload.put("userId", ACTOR.toString());
        payload.put("paymentId", ACTOR.toString());
        payload.put("totalAmount", new BigDecimal("100000.00"));
        payload.put("currency", "VND");
        payload.put("confirmedAt", NOW.toString());
        var lines = payload.putArray("seats");
        for (var seat : List.of(couple, single)) {
            var line = lines.addObject();
            line.put("seatNumber", seat.getSeatNumber());
            line.put("seatType", seat.getSeatType());
            line.put("price", seat.getPrice());
        }
        var event =
                new OutboxEventMessage(
                        ACTOR,
                        BOOKING,
                        "BOOKING",
                        "booking-confirmed",
                        "1",
                        NOW,
                        "booking-service",
                        SHOWTIME,
                        ACTOR,
                        payload);

        assertMismatch(() -> service.issue(BOOKING.toString(), event));
    }

    private BookingTicket wrongCapacity() {
        var ticket = mock(BookingTicket.class);
        when(ticket.getBookingSeatId()).thenReturn(couple.getId());
        when(ticket.getBookingId()).thenReturn(BOOKING);
        when(ticket.getSeatNumber()).thenReturn(couple.getSeatNumber());
        when(ticket.getCapacity()).thenReturn(1);
        return ticket;
    }

    private void assertMismatch(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(
                        ConflictException.class,
                        failure ->
                                assertThat(failure.getErrorCode())
                                        .isSameAs(BookingErrorCode.RESERVATION_RESULT_MISMATCH));
        verify(tickets, never()).save(any(BookingTicket.class));
    }

    private static BookingSeat seat(UUID showtimeId, String number, String type) {
        var seat = new BookingSeat(BOOKING, showtimeId, number);
        seat.completeSnapshot(UuidGenerator.next(), type, new BigDecimal("50000.00"));
        return seat;
    }
}
