package com.cinema.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.inventory.entity.Seat;
import com.cinema.inventory.entity.ShowSeat;
import com.cinema.inventory.entity.Showtime;
import com.cinema.inventory.enums.SeatType;
import com.cinema.inventory.enums.ShowSeatStatus;
import com.cinema.inventory.enums.ShowtimeStatus;
import com.cinema.inventory.event.InventoryEventContract;
import com.cinema.inventory.event.SeatReservationRejectedOutboxFactory;
import com.cinema.inventory.event.SeatReservationRejectionReason;
import com.cinema.inventory.event.SeatReservedOutboxFactory;
import com.cinema.inventory.event.payload.RequestedSeatPayload;
import com.cinema.inventory.event.payload.SeatReservationRequestedPayload;
import com.cinema.inventory.event.serialization.SeatReservationRequestedPayloadReader;
import com.cinema.inventory.event.validation.SeatReservationRequestedMessageValidator;
import com.cinema.inventory.repository.ShowSeatRepository;
import com.cinema.inventory.repository.ShowtimeRepository;
import com.cinema.inventory.service.impl.SeatReservationRequestedConsumerServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class SeatReservationLockedSetTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-11T04:00:00Z");
    private static final OffsetDateTime EXPIRY = NOW.plusMinutes(10);
    private final UUID bookingId = UuidGenerator.next();
    private final UUID showtimeId = UuidGenerator.next();
    private final ShowSeatRepository seats = mock(ShowSeatRepository.class);
    private final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
    private final SeatReservationRequestedPayloadReader reader = mock(SeatReservationRequestedPayloadReader.class);
    private final ProcessedEventRegistrationService processed = mock(ProcessedEventRegistrationService.class);
    private final SeatReservedOutboxFactory reserved = mock(SeatReservedOutboxFactory.class);
    private final SeatReservationRejectedOutboxFactory rejected = mock(SeatReservationRejectedOutboxFactory.class);
    private final SeatReservationPricingService pricing = mock(SeatReservationPricingService.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final OutboxEventEntity resultEvent = mock(OutboxEventEntity.class);
    private final SeatReservationRequestedConsumerService service = new SeatReservationRequestedConsumerServiceImpl(
            mock(SeatReservationRequestedMessageValidator.class), reader, processed, showtimes,
            seats, reserved, rejected, outbox, Clock.fixed(NOW.toInstant(), ZoneOffset.UTC), pricing);
    private final OutboxEventMessage message = new OutboxEventMessage(UuidGenerator.next(), bookingId,
            "BOOKING", InventoryEventContract.SEAT_RESERVATION_REQUESTED, "1", NOW,
            "booking-service", UuidGenerator.next(), null, null);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aMissingSeatRejectsTheEntireRequestBeforePricingOrHolding(boolean grouped) {
        var available = seat("H7");
        prepare(List.of(available), grouped);

        assertEquals(SeatReservationRequestedConsumerService.Status.REJECTED, handle().status());

        assertRejected(SeatReservationRejectionReason.SEAT_NOT_FOUND, List.of("H8"));
        assertEquals(ShowSeatStatus.AVAILABLE, available.getStatus());
        assertNull(available.getHeldByBookingId());
    }

    @Test
    void anEmptyResultCannotBeReportedAsAnEmptySuccessfulReservation() {
        prepare(List.of(), false);

        assertEquals(SeatReservationRequestedConsumerService.Status.REJECTED, handle().status());

        assertRejected(SeatReservationRejectionReason.SEAT_NOT_FOUND, List.of("H7", "H8"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OTHER_BOOKING", "BOOKED", "UNAVAILABLE", "EXPIRED", "DIFFERENT_EXPIRY"})
    void anUnusableSeatPreventsPartialHoldAndPreservesItsOwner(String reason) {
        var available = seat("H7");
        var occupied = seat("H8");
        UUID owner = reason.equals("OTHER_BOOKING") ? UuidGenerator.next() : bookingId;
        if (reason.equals("UNAVAILABLE")) {
            occupied.makeUnavailable();
        } else {
            var expiry = reason.equals("EXPIRED") ? NOW.minusMinutes(1)
                    : reason.equals("DIFFERENT_EXPIRY") ? EXPIRY.plusMinutes(1) : EXPIRY;
            occupied.hold(owner, expiry, NOW.minusMinutes(20));
            if (reason.equals("BOOKED")) occupied.book(owner);
        }
        var previousStatus = occupied.getStatus();
        var previousOwner = occupied.getHeldByBookingId();
        var previousExpiry = occupied.getHoldExpiresAt();
        prepare(List.of(available, occupied), true);

        assertEquals(SeatReservationRequestedConsumerService.Status.REJECTED, handle().status());

        assertRejected(SeatReservationRejectionReason.SEAT_UNAVAILABLE, List.of("H8"));
        assertEquals(ShowSeatStatus.AVAILABLE, available.getStatus());
        assertNull(available.getHeldByBookingId());
        assertEquals(previousStatus, occupied.getStatus());
        assertEquals(previousOwner, occupied.getHeldByBookingId());
        assertEquals(previousExpiry, occupied.getHoldExpiresAt());
    }

    @Test
    void sameBookingHoldAtTheSameInstantCanBeReusedAlongsideAnAvailableSeat() {
        var available = seat("H7");
        var owned = seat("H8");
        var storedExpiry = EXPIRY.withOffsetSameInstant(ZoneOffset.ofHours(7));
        owned.hold(bookingId, storedExpiry, NOW);
        prepare(List.of(available, owned), false);

        assertEquals(SeatReservationRequestedConsumerService.Status.RESERVED, handle().status());

        assertEquals(ShowSeatStatus.HELD, available.getStatus());
        assertEquals(bookingId, available.getHeldByBookingId());
        assertEquals(EXPIRY, available.getHoldExpiresAt());
        assertEquals(storedExpiry, owned.getHoldExpiresAt());
        assertEquals(bookingId, owned.getHeldByBookingId());
        verify(seats).saveAll(List.of(available, owned));
        verifyNoInteractions(rejected, pricing);
    }

    private SeatReservationRequestedConsumerService.Result handle() {
        return service.handle(bookingId.toString(), message);
    }

    private void prepare(List<ShowSeat> locked, boolean grouped) {
        UUID groupId = grouped ? UuidGenerator.next() : null;
        var payload = new SeatReservationRequestedPayload(bookingId, UuidGenerator.next(), showtimeId,
                List.of(new RequestedSeatPayload(" h8 ", groupId), new RequestedSeatPayload("h7", groupId)),
                NOW.minusSeconds(1), EXPIRY);
        when(reader.read(message)).thenReturn(payload);
        when(processed.register(any(), anyString(), anyString(), anyString())).thenReturn(true);
        when(showtimes.existsHoldEligibleShowtime(showtimeId, NOW, ShowtimeStatus.OPEN_FOR_BOOKING)).thenReturn(true);
        when(seats.findAllByShowtimeIdAndSeatNumbersForUpdate(showtimeId, List.of("H7", "H8"))).thenReturn(locked);
        when(resultEvent.getId()).thenReturn(UuidGenerator.next());
        when(rejected.create(any(), any(), any(), anyString(), anyList(), any(), any(), any())).thenReturn(resultEvent);
        when(reserved.create(any(), any(), anyList(), any(), any(), any(), any())).thenReturn(resultEvent);
    }

    private void assertRejected(SeatReservationRejectionReason reason, List<String> affected) {
        verify(rejected).create(eq(bookingId), eq(showtimeId), eq(reason), anyString(),
                eq(affected), eq(NOW), eq(message.correlationId()), eq(message.eventId()));
        verify(outbox).save(resultEvent);
        verify(seats, never()).saveAll(anyList());
        verifyNoInteractions(reserved, pricing);
    }

    private ShowSeat seat(String number) {
        Seat seat = mock(Seat.class);
        when(seat.getSeatNumber()).thenReturn(number);
        when(seat.getSeatType()).thenReturn(SeatType.STANDARD);
        when(seat.isActive()).thenReturn(true);
        return new ShowSeat(mock(Showtime.class), seat, new BigDecimal("90000"));
    }
}
