package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.client.PrintSimulationAccessClient;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.entity.BookingTicket;
import com.cinema.booking.enums.BookingPickupStatus;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingPickupPassRepository;
import com.cinema.booking.repository.BookingPrintSimulationRepository;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.BookingTicketRepository;
import com.cinema.common.exception.exception.ServiceUnavailableException;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class BookingPickupAvailabilityTest {

    private static final UUID BOOKING_ID = UUID.fromString("019c0000-0000-7000-8000-000000000001");
    private static final UUID OWNER_ID = UUID.fromString("019c0000-0000-7000-8000-000000000002");
    private static final UUID SHOWTIME_ID = UUID.fromString("019c0000-0000-7000-8000-000000000003");

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingSeatRepository seats = mock(BookingSeatRepository.class);
    private final BookingTicketRepository tickets = mock(BookingTicketRepository.class);
    private final BookingPickupPassRepository passes = mock(BookingPickupPassRepository.class);
    private final BookingPrintSimulationRepository simulations =
            mock(BookingPrintSimulationRepository.class);
    private final BookingPickupTokenCodec tokens = mock(BookingPickupTokenCodec.class);
    private final PrintSimulationAccessClient client = mock(PrintSimulationAccessClient.class);
    private final Booking booking = mock(Booking.class);
    private final BookingPickupTransactionService transaction =
            new BookingPickupTransactionService(
                    bookings,
                    seats,
                    tickets,
                    passes,
                    simulations,
                    tokens,
                    Clock.systemUTC(),
                    false);
    private final BookingPickupService service =
            new BookingPickupService(bookings, passes, client, tokens, transaction);

    @Test
    void missingTicketsReturnWaitingWithoutIssuingCredentialWhenInventoryIsUnavailable() {
        unavailableSchedule();
        when(seats.findAllByBookingIdOrderBySeatNumberAsc(BOOKING_ID))
                .thenReturn(List.of(mock(BookingSeat.class), mock(BookingSeat.class)));

        var response = service.owner(OWNER_ID, BOOKING_ID);

        assertThat(response.status()).isEqualTo(BookingPickupStatus.WAITING_FOR_TICKETS);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.expectedTickets()).isEqualTo(2);
        assertThat(response.issuedTickets()).isEqualTo(1);
        assertThat(response.qrPayload()).isNull();
        assertThat(response.expiresAt()).isNull();
        verify(bookings).findByIdAndUserIdForUpdate(BOOKING_ID, OWNER_ID);
        verifyNoInteractions(passes, tokens, simulations);
    }

    @Test
    void matchingCountsDoNotMaskUnavailableScheduleOrIssueCredential() {
        unavailableSchedule();

        assertThatThrownBy(() -> service.owner(OWNER_ID, BOOKING_ID))
                .isInstanceOfSatisfying(
                        ServiceUnavailableException.class,
                        failure ->
                                assertThat(failure.getErrorCode())
                                        .isSameAs(BookingErrorCode.PICKUP_CONTEXT_UNAVAILABLE));
        verify(bookings).findByIdAndUserIdForUpdate(BOOKING_ID, OWNER_ID);
        verifyNoInteractions(passes, tokens, simulations);
    }

    @Test
    void cancellationDuringLookupReturnsCurrentStatusWithoutCredential() {
        unavailableSchedule();
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED, BookingStatus.CANCELLED);

        var response = service.owner(OWNER_ID, BOOKING_ID);

        assertThat(response.status()).isEqualTo(BookingPickupStatus.NOT_CONFIRMED);
        assertThat(response.bookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.qrPayload()).isNull();
        assertThat(response.expiresAt()).isNull();
        verify(bookings).findByIdAndUserIdForUpdate(BOOKING_ID, OWNER_ID);
        verifyNoInteractions(passes, tokens, simulations);
    }

    private void unavailableSchedule() {
        when(bookings.findByIdAndUserId(BOOKING_ID, OWNER_ID)).thenReturn(Optional.of(booking));
        when(bookings.findByIdAndUserIdForUpdate(BOOKING_ID, OWNER_ID))
                .thenReturn(Optional.of(booking));
        when(booking.getId()).thenReturn(BOOKING_ID);
        when(booking.getStatus()).thenReturn(BookingStatus.CONFIRMED);
        when(booking.getShowtimeId()).thenReturn(SHOWTIME_ID);
        when(seats.findAllByBookingIdOrderBySeatNumberAsc(BOOKING_ID))
                .thenReturn(List.of(mock(BookingSeat.class)));
        when(tickets.findAllByBookingIdOrderBySeatNumberAsc(BOOKING_ID))
                .thenReturn(List.of(mock(BookingTicket.class)));
        when(client.pickupSchedule(SHOWTIME_ID))
                .thenThrow(
                        new ServiceUnavailableException(
                                BookingErrorCode.PICKUP_CONTEXT_UNAVAILABLE));
    }
}
