package com.cinema.inventory.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.inventory.dto.request.HoldShowSeatRequest;
import com.cinema.inventory.dto.request.ShowSeatBookingRequest;
import com.cinema.inventory.dto.response.ShowSeatResponse;
import com.cinema.inventory.entity.Cinema;
import com.cinema.inventory.entity.Room;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.RoomType;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.mapper.ShowSeatMapper;
import com.cinema.inventory.repository.SeatRepository;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.ShowSeatService;

@ExtendWith(MockitoExtension.class)
class ShowSeatTransitionServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse(
            "2099-01-01T03:00:00Z");

    private static final BigDecimal PRICE = new BigDecimal("120000.00");

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ShowSeatMapper showSeatMapper;

    private ShowSeatService showSeatService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                NOW.toInstant(),
                ZoneOffset.UTC);

        showSeatService = new ShowSeatServiceImpl(
                showSeatRepository,
                showtimeRepository,
                seatRepository,
                showSeatMapper,
                clock);
    }

    @Test
    void holdShouldTransitionAvailableSeatToHeld() {
        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        OffsetDateTime expiresAt = NOW.plusMinutes(10);

        ShowSeat showSeat = availableShowSeat();

        HoldShowSeatRequest request = new HoldShowSeatRequest(
                bookingId,
                expiresAt);

        ShowSeatResponse mappedResponse = response(
                ShowSeatStatus.HELD,
                bookingId,
                expiresAt);

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        when(showSeatMapper.toResponse(showSeat))
                .thenReturn(mappedResponse);

        ShowSeatResponse result = showSeatService.hold(
                showSeatId,
                request);

        assertThat(result)
                .isSameAs(mappedResponse);

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(showSeat.getHeldByBookingId())
                .isEqualTo(bookingId);

        assertThat(showSeat.getHoldExpiresAt())
                .isEqualTo(expiresAt);

        verify(showSeatRepository)
                .findByIdForUpdate(showSeatId);

        verify(showSeatMapper)
                .toResponse(showSeat);

        verify(showSeatRepository, never())
                .findById(showSeatId);
    }

    @Test
    void holdShouldRejectSeatThatIsAlreadyHeld() {
        UUID showSeatId = UUID.randomUUID();

        ShowSeat showSeat = heldShowSeat(
                UUID.randomUUID(),
                NOW.plusMinutes(10));

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.hold(
                showSeatId,
                new HoldShowSeatRequest(
                        UUID.randomUUID(),
                        NOW.plusMinutes(20))))
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Show seat is not available");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void holdShouldRejectPastExpiration() {
        UUID showSeatId = UUID.randomUUID();
        ShowSeat showSeat = availableShowSeat();

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.hold(
                showSeatId,
                new HoldShowSeatRequest(
                        UUID.randomUUID(),
                        NOW.minusSeconds(1))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Hold expiration must be in the future");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.AVAILABLE);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void holdShouldThrowWhenShowSeatDoesNotExist() {
        UUID showSeatId = UUID.randomUUID();

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> showSeatService.hold(
                showSeatId,
                new HoldShowSeatRequest(
                        UUID.randomUUID(),
                        NOW.plusMinutes(10))))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Show seat not found");

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void bookShouldTransitionOwnedHeldSeatToBooked() {
        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeat showSeat = heldShowSeat(
                bookingId,
                NOW.plusMinutes(10));

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(
                bookingId);

        ShowSeatResponse mappedResponse = response(
                ShowSeatStatus.BOOKED,
                null,
                null);

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        when(showSeatMapper.toResponse(showSeat))
                .thenReturn(mappedResponse);

        ShowSeatResponse result = showSeatService.book(
                showSeatId,
                request);

        assertThat(result)
                .isSameAs(mappedResponse);

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.BOOKED);

        assertThat(showSeat.getHeldByBookingId())
                .isNull();

        assertThat(showSeat.getHoldExpiresAt())
                .isNull();

        verify(showSeatRepository)
                .findByIdForUpdate(showSeatId);

        verify(showSeatMapper)
                .toResponse(showSeat);
    }

    @Test
    void bookShouldRejectAvailableSeat() {
        UUID showSeatId = UUID.randomUUID();
        ShowSeat showSeat = availableShowSeat();

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.book(
                showSeatId,
                new ShowSeatBookingRequest(
                        UUID.randomUUID())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Show seat is not held");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.AVAILABLE);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void bookShouldRejectDifferentBooking() {
        UUID showSeatId = UUID.randomUUID();
        UUID ownerBookingId = UUID.randomUUID();

        ShowSeat showSeat = heldShowSeat(
                ownerBookingId,
                NOW.plusMinutes(10));

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.book(
                showSeatId,
                new ShowSeatBookingRequest(
                        UUID.randomUUID())))
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Show seat is held by another booking");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(showSeat.getHeldByBookingId())
                .isEqualTo(ownerBookingId);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void bookShouldRejectExpiredHold() {
        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeat showSeat = availableShowSeat();

        showSeat.hold(
                bookingId,
                NOW.minusMinutes(1),
                NOW.minusMinutes(10));

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.book(
                showSeatId,
                new ShowSeatBookingRequest(
                        bookingId)))
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Show seat hold has expired");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(showSeat.getHeldByBookingId())
                .isEqualTo(bookingId);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void releaseShouldTransitionOwnedHeldSeatToAvailable() {
        UUID showSeatId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        ShowSeat showSeat = heldShowSeat(
                bookingId,
                NOW.plusMinutes(10));

        ShowSeatBookingRequest request = new ShowSeatBookingRequest(
                bookingId);

        ShowSeatResponse mappedResponse = response(
                ShowSeatStatus.AVAILABLE,
                null,
                null);

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        when(showSeatMapper.toResponse(showSeat))
                .thenReturn(mappedResponse);

        ShowSeatResponse result = showSeatService.release(
                showSeatId,
                request);

        assertThat(result)
                .isSameAs(mappedResponse);

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.AVAILABLE);

        assertThat(showSeat.getHeldByBookingId())
                .isNull();

        assertThat(showSeat.getHoldExpiresAt())
                .isNull();

        verify(showSeatRepository)
                .findByIdForUpdate(showSeatId);

        verify(showSeatMapper)
                .toResponse(showSeat);
    }

    @Test
    void releaseShouldRejectAvailableSeat() {
        UUID showSeatId = UUID.randomUUID();
        ShowSeat showSeat = availableShowSeat();

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.release(
                showSeatId,
                new ShowSeatBookingRequest(
                        UUID.randomUUID())))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Show seat is not held");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.AVAILABLE);

        verifyNoInteractions(showSeatMapper);
    }

    @Test
    void releaseShouldRejectDifferentBooking() {
        UUID showSeatId = UUID.randomUUID();
        UUID ownerBookingId = UUID.randomUUID();

        ShowSeat showSeat = heldShowSeat(
                ownerBookingId,
                NOW.plusMinutes(10));

        when(showSeatRepository
                .findByIdForUpdate(showSeatId))
                .thenReturn(Optional.of(showSeat));

        assertThatThrownBy(() -> showSeatService.release(
                showSeatId,
                new ShowSeatBookingRequest(
                        UUID.randomUUID())))
                .isInstanceOf(ConflictException.class)
                .hasMessage(
                        "Show seat is held by another booking");

        assertThat(showSeat.getStatus())
                .isEqualTo(ShowSeatStatus.HELD);

        assertThat(showSeat.getHeldByBookingId())
                .isEqualTo(ownerBookingId);

        verifyNoInteractions(showSeatMapper);
    }

    private ShowSeat heldShowSeat(
            UUID bookingId,
            OffsetDateTime expiresAt) {

        ShowSeat showSeat = availableShowSeat();

        showSeat.hold(
                bookingId,
                expiresAt,
                NOW.minusMinutes(1));

        return showSeat;
    }

    private ShowSeat availableShowSeat() {
        Cinema cinema = new Cinema(
                "CGV Vincom",
                "72 Le Thanh Ton",
                "Ho Chi Minh");

        Room room = new Room(
                cinema,
                "Room 01",
                RoomType.STANDARD);

        Seat seat = new Seat(
                room,
                "A1",
                "A",
                SeatType.STANDARD);

        Showtime showtime = new Showtime(
                UUID.randomUUID(),
                room,
                NOW.plusDays(1),
                NOW.plusDays(1).plusHours(2));

        return new ShowSeat(
                showtime,
                seat,
                PRICE);
    }

    private ShowSeatResponse response(
            ShowSeatStatus status,
            UUID heldByBookingId,
            OffsetDateTime holdExpiresAt) {

        return new ShowSeatResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A1",
                SeatType.STANDARD,
                PRICE,
                status,
                heldByBookingId,
                holdExpiresAt,
                NOW,
                NOW);
    }
}
