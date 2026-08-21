package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.event.SeatReservationRequestedOutboxFactory;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.service.OutboxService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class BookingCreationServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private BookingMapper bookingMapper;

    @Mock private SeatReservationRequestedOutboxFactory outboxFactory;

    @Mock private OutboxService outboxService;

    private BookingProperties bookingProperties;

    private BookingCreationServiceImpl bookingCreationService;

    @BeforeEach
    void setUp() {

        MockitoAnnotations.openMocks(this);

        bookingProperties = new BookingProperties(Duration.ofMinutes(10), 10);

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        bookingCreationService =
                new BookingCreationServiceImpl(
                        bookingRepository,
                        bookingSeatRepository,
                        bookingMapper,
                        bookingProperties,
                        outboxFactory,
                        outboxService,
                        clock);
    }

    @Test
    void shouldCreateBookingSeatsAndOutboxEventInOrder() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        List<String> normalizedSeatNumbers = List.of("H7", "H8");

        BookingResponse expectedResponse = mock(BookingResponse.class);

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingSeatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(outboxFactory.create(any(Booking.class), anyList(), eq(NOW_OFFSET)))
                .thenReturn(outboxEvent);

        when(bookingMapper.toResponse(any(Booking.class), anyList())).thenReturn(expectedResponse);

        BookingResponse actualResponse =
                bookingCreationService.createNew(
                        userId, showtimeId, "request-1", "a".repeat(64), normalizedSeatNumbers);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingSeat>> seatsCaptor = ArgumentCaptor.forClass(List.class);

        verify(bookingRepository).saveAndFlush(bookingCaptor.capture());

        verify(bookingSeatRepository).saveAll(seatsCaptor.capture());

        Booking savedBooking = bookingCaptor.getValue();

        List<BookingSeat> savedSeats = seatsCaptor.getValue();

        assertThat(savedBooking.getUserId()).isEqualTo(userId);

        assertThat(savedBooking.getShowtimeId()).isEqualTo(showtimeId);

        assertThat(savedBooking.getClientRequestId()).isEqualTo("request-1");

        assertThat(savedBooking.getRequestFingerprint()).isEqualTo("a".repeat(64));

        assertThat(savedBooking.getExpiresAt()).isEqualTo(NOW_OFFSET.plusMinutes(10));

        assertThat(savedSeats).extracting(BookingSeat::getSeatNumber).containsExactly("H7", "H8");

        assertThat(savedSeats)
                .extracting(BookingSeat::getBookingId)
                .containsOnly(savedBooking.getId());

        assertThat(savedSeats).extracting(BookingSeat::getShowtimeId).containsOnly(showtimeId);

        verify(outboxFactory).create(savedBooking, savedSeats, NOW_OFFSET);

        verify(outboxService).save(outboxEvent);

        verify(bookingMapper).toResponse(savedBooking, savedSeats);

        assertSame(expectedResponse, actualResponse);

        InOrder order =
                inOrder(
                        bookingRepository,
                        bookingSeatRepository,
                        outboxFactory,
                        outboxService,
                        bookingMapper);

        order.verify(bookingRepository).saveAndFlush(savedBooking);

        order.verify(bookingSeatRepository).saveAll(savedSeats);

        order.verify(outboxFactory).create(savedBooking, savedSeats, NOW_OFFSET);

        order.verify(outboxService).save(outboxEvent);

        order.verify(bookingMapper).toResponse(savedBooking, savedSeats);

        verifyNoMoreInteractions(
                bookingRepository,
                bookingSeatRepository,
                outboxFactory,
                outboxService,
                bookingMapper);
    }

    @Test
    void outboxFactoryFailureShouldPropagateBeforeResponseMapping() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        InternalServerException failure = mock(InternalServerException.class);

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingSeatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(outboxFactory.create(any(Booking.class), anyList(), eq(NOW_OFFSET)))
                .thenThrow(failure);

        InternalServerException actualException =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                bookingCreationService.createNew(
                                        userId,
                                        showtimeId,
                                        "request-1",
                                        "a".repeat(64),
                                        List.of("H7")));

        assertSame(failure, actualException);

        verify(bookingRepository).saveAndFlush(any(Booking.class));

        verify(bookingSeatRepository).saveAll(anyList());

        verify(outboxFactory).create(any(Booking.class), anyList(), eq(NOW_OFFSET));

        verifyNoMoreInteractions(bookingRepository, bookingSeatRepository, outboxFactory);

        verifyNoMoreInteractions(outboxService, bookingMapper);
    }

    @Test
    void outboxSaveFailureShouldPropagateBeforeResponseMapping() {

        UUID userId = UUID.randomUUID();
        UUID showtimeId = UUID.randomUUID();

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        InternalServerException failure = mock(InternalServerException.class);

        when(bookingRepository.saveAndFlush(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingSeatRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(outboxFactory.create(any(Booking.class), anyList(), eq(NOW_OFFSET)))
                .thenReturn(outboxEvent);

        org.mockito.Mockito.doThrow(failure).when(outboxService).save(outboxEvent);

        InternalServerException actualException =
                assertThrows(
                        InternalServerException.class,
                        () ->
                                bookingCreationService.createNew(
                                        userId,
                                        showtimeId,
                                        "request-1",
                                        "a".repeat(64),
                                        List.of("H7")));

        assertSame(failure, actualException);

        verify(outboxService).save(outboxEvent);

        verifyNoMoreInteractions(bookingMapper);
    }
}
