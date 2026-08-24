package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingCancellationReason;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingCancelledOutboxFactory;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class BookingCancellationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private BookingMapper bookingMapper;

    @Mock private BookingCancelledOutboxFactory outboxFactory;

    @Mock private OutboxService outboxService;

    private BookingCancellationServiceImpl cancellationService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        cancellationService =
                new BookingCancellationServiceImpl(
                        bookingRepository,
                        bookingSeatRepository,
                        bookingMapper,
                        outboxFactory,
                        outboxService,
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldCancelOwnedReservedBookingAndCreateOutboxEvent() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        Booking booking = reservedBooking(userId, showtimeId, NOW_OFFSET.plusMinutes(10));

        List<BookingSeat> bookingSeats =
                List.of(new BookingSeat(booking.getId(), showtimeId, "H7"));

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        BookingResponse expectedResponse = mock(BookingResponse.class);

        when(bookingRepository.findByIdAndUserIdForUpdate(booking.getId(), userId))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.saveAndFlush(booking)).thenReturn(booking);

        when(outboxFactory.create(booking, BookingCancellationReason.USER_REQUESTED))
                .thenReturn(outboxEvent);

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId()))
                .thenReturn(bookingSeats);

        when(bookingMapper.toResponse(booking, bookingSeats)).thenReturn(expectedResponse);

        BookingResponse actualResponse = cancellationService.cancel(userId, booking.getId());

        assertSame(expectedResponse, actualResponse);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(booking.getCancelledAt()).isEqualTo(NOW_OFFSET);

        verify(outboxFactory).create(booking, BookingCancellationReason.USER_REQUESTED);

        verify(outboxService).save(outboxEvent);

        InOrder order =
                inOrder(
                        bookingRepository,
                        outboxFactory,
                        outboxService,
                        bookingSeatRepository,
                        bookingMapper);

        order.verify(bookingRepository).findByIdAndUserIdForUpdate(booking.getId(), userId);

        order.verify(bookingRepository).saveAndFlush(booking);

        order.verify(outboxFactory).create(booking, BookingCancellationReason.USER_REQUESTED);

        order.verify(outboxService).save(outboxEvent);

        order.verify(bookingSeatRepository).findAllByBookingIdOrderBySeatNumberAsc(booking.getId());

        order.verify(bookingMapper).toResponse(booking, bookingSeats);
    }

    @Test
    void missingOrForeignBookingShouldReturnNotFound() {

        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        when(bookingRepository.findByIdAndUserIdForUpdate(bookingId, userId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cancellationService.cancel(userId, bookingId));

        verify(bookingRepository).findByIdAndUserIdForUpdate(bookingId, userId);

        verify(bookingRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());

        verifyNoInteractions(outboxFactory, outboxService, bookingSeatRepository, bookingMapper);
    }

    @Test
    void pendingBookingShouldNotCreateCancellationOutboxEvent() {

        UUID userId = UUID.randomUUID();

        Booking booking =
                new Booking(
                        userId,
                        UUID.randomUUID(),
                        "request-1",
                        "a".repeat(64),
                        NOW_OFFSET.plusMinutes(10),
                        NOW_OFFSET.minusMinutes(1));

        when(bookingRepository.findByIdAndUserIdForUpdate(booking.getId(), userId))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ConflictException.class, () -> cancellationService.cancel(userId, booking.getId()));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        verify(bookingRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());

        verifyNoInteractions(outboxFactory, outboxService, bookingSeatRepository, bookingMapper);
    }

    @Test
    void expiredReservationShouldNotCreateCancellationOutboxEvent() {

        UUID userId = UUID.randomUUID();

        Booking booking = reservedBooking(userId, UUID.randomUUID(), NOW_OFFSET);

        when(bookingRepository.findByIdAndUserIdForUpdate(booking.getId(), userId))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ConflictException.class, () -> cancellationService.cancel(userId, booking.getId()));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getCancelledAt()).isNull();

        verifyNoInteractions(outboxFactory, outboxService, bookingSeatRepository, bookingMapper);
    }

    @Test
    void nullUserIdShouldUseValidationException() {

        assertThrows(
                ValidationException.class,
                () -> cancellationService.cancel(null, UUID.randomUUID()));

        verifyNoInteractions(
                bookingRepository,
                bookingSeatRepository,
                bookingMapper,
                outboxFactory,
                outboxService);
    }

    @Test
    void nullBookingIdShouldUseValidationException() {

        assertThrows(
                ValidationException.class,
                () -> cancellationService.cancel(UUID.randomUUID(), null));

        verifyNoInteractions(
                bookingRepository,
                bookingSeatRepository,
                bookingMapper,
                outboxFactory,
                outboxService);
    }

    private static Booking reservedBooking(UUID userId, UUID showtimeId, OffsetDateTime expiresAt) {

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "request-1",
                        "a".repeat(64),
                        expiresAt,
                        NOW_OFFSET.minusMinutes(20));

        booking.reserve(new BigDecimal("250000.00"), "VND");

        return booking;
    }
}
