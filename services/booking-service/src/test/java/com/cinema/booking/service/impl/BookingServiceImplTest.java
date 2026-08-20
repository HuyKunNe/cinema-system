package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.config.BookingProperties;
import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.idempotency.BookingRequestFingerprint;
import com.cinema.booking.mapper.BookingMapper;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCreationService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class BookingServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-20T10:00:00Z");

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private BookingCreationService bookingCreationService;

    @Mock private BookingMapper bookingMapper;

    private BookingRequestFingerprint requestFingerprint;

    private BookingProperties bookingProperties;

    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        requestFingerprint = new BookingRequestFingerprint();

        bookingProperties = new BookingProperties(Duration.ofMinutes(10), 10);

        bookingService =
                new BookingServiceImpl(
                        bookingRepository,
                        bookingSeatRepository,
                        bookingCreationService,
                        requestFingerprint,
                        bookingMapper,
                        bookingProperties);
    }

    @Test
    void newRequestShouldDelegateNormalizedSortedRequestToCreationService() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest("  request-1  ", showtimeId, List.of(" h8 ", "h7"));

        String expectedFingerprint = requestFingerprint.generate(showtimeId, List.of("H7", "H8"));

        BookingResponse expectedResponse = response(bookingId, userId, showtimeId, "request-1");

        when(bookingRepository.findByUserIdAndClientRequestId(userId, "request-1"))
                .thenReturn(Optional.empty());

        when(bookingCreationService.createNew(
                        eq(userId),
                        eq(showtimeId),
                        eq("request-1"),
                        eq(expectedFingerprint),
                        org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(expectedResponse);

        BookingResponse actualResponse = bookingService.create(userId, request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> seatsCaptor = ArgumentCaptor.forClass(List.class);

        verify(bookingCreationService)
                .createNew(
                        eq(userId),
                        eq(showtimeId),
                        eq("request-1"),
                        eq(expectedFingerprint),
                        seatsCaptor.capture());

        assertThat(seatsCaptor.getValue()).containsExactly("H7", "H8");

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void sameRequestShouldReturnExistingBooking() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        List<String> normalizedSeats = List.of("H7", "H8");

        String fingerprint = requestFingerprint.generate(showtimeId, normalizedSeats);

        Booking existingBooking =
                new Booking(userId, showtimeId, "request-1", fingerprint, NOW.plusMinutes(10), NOW);

        BookingResponse expectedResponse =
                response(existingBooking.getId(), userId, showtimeId, "request-1");

        when(bookingRepository.findByUserIdAndClientRequestId(userId, "request-1"))
                .thenReturn(Optional.of(existingBooking));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(existingBooking.getId()))
                .thenReturn(List.of());

        when(bookingMapper.toResponse(existingBooking, List.of())).thenReturn(expectedResponse);

        BookingResponse actualResponse =
                bookingService.create(
                        userId,
                        new CreateBookingRequest("request-1", showtimeId, List.of("H8", "H7")));

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verifyNoInteractions(bookingCreationService);
    }

    @Test
    void reusedRequestIdWithDifferentPayloadShouldConflict() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        String existingFingerprint = requestFingerprint.generate(showtimeId, List.of("H7"));

        Booking existingBooking =
                new Booking(
                        userId,
                        showtimeId,
                        "request-1",
                        existingFingerprint,
                        NOW.plusMinutes(10),
                        NOW);

        when(bookingRepository.findByUserIdAndClientRequestId(userId, "request-1"))
                .thenReturn(Optional.of(existingBooking));

        CreateBookingRequest differentRequest =
                new CreateBookingRequest("request-1", showtimeId, List.of("H8"));

        assertThrows(
                ConflictException.class, () -> bookingService.create(userId, differentRequest));

        verifyNoInteractions(bookingCreationService);

        verifyNoInteractions(bookingSeatRepository);

        verifyNoInteractions(bookingMapper);
    }

    @Test
    void differentSeatOrderShouldProduceSameLogicalRequest() {
        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        String existingFingerprint = requestFingerprint.generate(showtimeId, List.of("H7", "H8"));

        Booking existingBooking =
                new Booking(
                        userId,
                        showtimeId,
                        "request-1",
                        existingFingerprint,
                        NOW.plusMinutes(10),
                        NOW);

        BookingResponse expectedResponse =
                response(existingBooking.getId(), userId, showtimeId, "request-1");

        when(bookingRepository.findByUserIdAndClientRequestId(userId, "request-1"))
                .thenReturn(Optional.of(existingBooking));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(existingBooking.getId()))
                .thenReturn(List.of());

        when(bookingMapper.toResponse(existingBooking, List.of())).thenReturn(expectedResponse);

        BookingResponse response =
                bookingService.create(
                        userId,
                        new CreateBookingRequest("request-1", showtimeId, List.of("H8", "H7")));

        assertThat(response.id()).isEqualTo(existingBooking.getId());

        verifyNoInteractions(bookingCreationService);
    }

    @Test
    void duplicateNormalizedSeatNumbersShouldBeRejected() {
        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7", " h7 "));

        assertThrows(
                ValidationException.class,
                () -> bookingService.create(UuidGenerator.next(), request));

        verifyNoInteractions(bookingCreationService);

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void seatCountAboveConfiguredLimitShouldBeRejected() {
        bookingProperties = new BookingProperties(Duration.ofMinutes(10), 1);

        bookingService =
                new BookingServiceImpl(
                        bookingRepository,
                        bookingSeatRepository,
                        bookingCreationService,
                        requestFingerprint,
                        bookingMapper,
                        bookingProperties);

        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7", "H8"));

        assertThrows(
                ValidationException.class,
                () -> bookingService.create(UuidGenerator.next(), request));

        verifyNoInteractions(bookingCreationService);

        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldLoadBookingOnlyForAuthenticatedOwner() {
        UUID userId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        String fingerprint = requestFingerprint.generate(showtimeId, List.of("H7"));

        Booking booking =
                new Booking(userId, showtimeId, "request-1", fingerprint, NOW.plusMinutes(10), NOW);

        BookingResponse expectedResponse =
                response(booking.getId(), userId, showtimeId, "request-1");

        when(bookingRepository.findByIdAndUserId(bookingId, userId))
                .thenReturn(Optional.of(booking));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(booking.getId()))
                .thenReturn(List.of());

        when(bookingMapper.toResponse(booking, List.of())).thenReturn(expectedResponse);

        BookingResponse actualResponse = bookingService.findById(userId, bookingId);

        assertThat(actualResponse).isEqualTo(expectedResponse);

        verify(bookingRepository).findByIdAndUserId(bookingId, userId);
    }

    @Test
    void bookingNotOwnedByUserShouldReturnNotFound() {
        UUID userId = UuidGenerator.next();
        UUID bookingId = UuidGenerator.next();

        when(bookingRepository.findByIdAndUserId(bookingId, userId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.findById(userId, bookingId));

        verifyNoInteractions(bookingSeatRepository);

        verifyNoInteractions(bookingMapper);
    }

    @Test
    void missingAuthenticatedUserShouldBeRejected() {
        CreateBookingRequest request =
                new CreateBookingRequest("request-1", UuidGenerator.next(), List.of("H7"));

        assertThrows(ValidationException.class, () -> bookingService.create(null, request));

        verifyNoInteractions(bookingRepository);

        verifyNoInteractions(bookingCreationService);
    }

    private BookingResponse response(
            UUID bookingId, UUID userId, UUID showtimeId, String clientRequestId) {

        return new BookingResponse(
                bookingId,
                userId,
                showtimeId,
                clientRequestId,
                BookingStatus.PENDING,
                null,
                null,
                NOW.plusMinutes(10),
                null,
                null,
                null,
                List.of(),
                0L,
                NOW,
                NOW);
    }
}
