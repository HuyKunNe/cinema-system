package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class BookingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private BookingMapper bookingMapper;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        BookingProperties properties = new BookingProperties(Duration.ofMinutes(10), 10);

        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        bookingService =
                new BookingServiceImpl(
                        bookingRepository, bookingSeatRepository, bookingMapper, properties, clock);
    }

    @Test
    void shouldCreatePendingBookingForAuthenticatedUser() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", showtimeId, List.of(" h8 ", "h7"));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingSeatRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.create(userId, request);

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);

        verify(bookingRepository).save(bookingCaptor.capture());

        Booking savedBooking = bookingCaptor.getValue();

        assertThat(savedBooking.getUserId()).isEqualTo(userId);
        assertThat(savedBooking.getShowtimeId()).isEqualTo(showtimeId);
        assertThat(savedBooking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getExpiresAt())
                .isEqualTo(NOW.atOffset(ZoneOffset.UTC).plusMinutes(10));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BookingSeat>> seatsCaptor = ArgumentCaptor.forClass(List.class);

        verify(bookingSeatRepository).saveAll(seatsCaptor.capture());

        assertThat(seatsCaptor.getValue())
                .extracting(BookingSeat::getSeatNumber)
                .containsExactly("H8", "H7");
    }

    @Test
    void shouldRejectDuplicateNormalizedSeatNumbers() {
        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7", " h7 "));

        assertThrows(
                ValidationException.class,
                () -> bookingService.create(UuidGenerator.next(), request));
    }

    @Test
    void shouldRejectSeatCountAboveConfiguredLimit() {
        BookingProperties properties = new BookingProperties(Duration.ofMinutes(10), 1);

        bookingService =
                new BookingServiceImpl(
                        bookingRepository,
                        bookingSeatRepository,
                        bookingMapper,
                        properties,
                        Clock.fixed(NOW, ZoneOffset.UTC));

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7", "H8"));

        assertThrows(
                ValidationException.class,
                () -> bookingService.create(UuidGenerator.next(), request));
    }

    @Test
    void shouldLoadBookingOnlyForAuthenticatedOwner() {
        UUID userId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        UuidGenerator.next(),
                        "request-1",
                        NOW.atOffset(ZoneOffset.UTC).plusMinutes(10),
                        NOW.atOffset(ZoneOffset.UTC));

        when(bookingRepository.findByIdAndUserId(bookingId, userId))
                .thenReturn(Optional.of(booking));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId()))
                .thenReturn(List.of());

        bookingService.findById(userId, bookingId);

        verify(bookingRepository).findByIdAndUserId(bookingId, userId);
    }

    @Test
    void shouldReturnNotFoundForBookingNotOwnedByUser() {
        UUID userId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();

        when(bookingRepository.findByIdAndUserId(bookingId, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.findById(userId, bookingId));
    }
}
