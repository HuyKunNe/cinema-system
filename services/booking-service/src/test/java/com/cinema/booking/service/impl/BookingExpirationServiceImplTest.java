package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingExpiredOutboxFactory;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.service.OutboxService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

class BookingExpirationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingExpiredOutboxFactory outboxFactory;

    @Mock private OutboxService outboxService;

    private BookingExpirationServiceImpl expirationService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        expirationService =
                new BookingExpirationServiceImpl(
                        bookingRepository,
                        outboxFactory,
                        outboxService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void dueReservedBookingShouldExpireAndCreateOutbox() {

        Booking booking = reservedBooking(NOW_OFFSET.minusSeconds(1));

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        when(outboxFactory.create(booking, NOW_OFFSET)).thenReturn(outboxEvent);

        boolean expired = expirationService.expireIfDue(booking.getId());

        assertThat(expired).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        verify(outboxService).save(outboxEvent);

        InOrder order = inOrder(bookingRepository, outboxFactory, outboxService);

        order.verify(bookingRepository).findByIdForUpdate(booking.getId());

        order.verify(bookingRepository).saveAndFlush(booking);

        order.verify(outboxFactory).create(booking, NOW_OFFSET);

        order.verify(outboxService).save(outboxEvent);
    }

    @Test
    void reservationExpiringExactlyNowShouldExpire() {

        Booking booking = reservedBooking(NOW_OFFSET);

        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        when(outboxFactory.create(booking, NOW_OFFSET)).thenReturn(mock(OutboxEventEntity.class));

        assertThat(expirationService.expireIfDue(booking.getId())).isTrue();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void futureReservationShouldNotExpire() {

        Booking booking = reservedBooking(NOW_OFFSET.plusSeconds(1));

        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        boolean expired = expirationService.expireIfDue(booking.getId());

        assertThat(expired).isFalse();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        verify(bookingRepository, never()).saveAndFlush(booking);

        verifyNoInteractions(outboxFactory, outboxService);
    }

    @Test
    void pendingBookingShouldNotExpire() {

        Booking booking =
                new Booking(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "request-1",
                        "a".repeat(64),
                        NOW_OFFSET.plusMinutes(10),
                        NOW_OFFSET.minusMinutes(20));

        when(bookingRepository.findByIdForUpdate(booking.getId())).thenReturn(Optional.of(booking));

        assertThat(expirationService.expireIfDue(booking.getId())).isFalse();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        verifyNoInteractions(outboxFactory, outboxService);
    }

    @Test
    void missingBookingShouldBeIgnored() {

        UUID bookingId = UUID.randomUUID();

        when(bookingRepository.findByIdForUpdate(bookingId)).thenReturn(Optional.empty());

        assertThat(expirationService.expireIfDue(bookingId)).isFalse();

        verifyNoInteractions(outboxFactory, outboxService);
    }

    @Test
    void nullBookingIdShouldUseValidationException() {

        assertThrows(ValidationException.class, () -> expirationService.expireIfDue(null));

        verifyNoInteractions(bookingRepository, outboxFactory, outboxService);
    }

    private static Booking reservedBooking(OffsetDateTime expiresAt) {

        Booking booking =
                new Booking(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "request-1",
                        "a".repeat(64),
                        expiresAt,
                        NOW_OFFSET.minusMinutes(20));

        booking.reserve(new BigDecimal("250000.00"), "VND");

        return booking;
    }
}
