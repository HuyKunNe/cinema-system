package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.event.serialization.SeatReservationRejectedPayloadReader;
import com.cinema.booking.event.validation.SeatReservationRejectedMessageValidator;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SeatReservationRejectedConsumerServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    @Mock private SeatReservationRejectedMessageValidator messageValidator;

    @Mock private SeatReservationRejectedPayloadReader payloadReader;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private BookingRepository bookingRepository;

    private SeatReservationRejectedConsumerServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new SeatReservationRejectedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        processedEventRegistrationService,
                        bookingRepository);
    }

    @Test
    void validEventShouldRejectPendingBooking() {

        TestContext context = validContext();

        prepareSuccessfulProcessing(context);

        SeatReservationRejectedConsumerService.Result result =
                service.handle(context.bookingId().toString(), context.message());

        assertThat(result.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.REJECTED);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(context.booking().getRejectionReason()).isEqualTo("SEAT_UNAVAILABLE");

        verify(bookingRepository).save(context.booking());
    }

    @Test
    void duplicateEventShouldNotLoadBooking() {

        TestContext context = validContext();

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        "booking-seat-rejected",
                        "seat-reservation-rejected",
                        "1"))
                .thenReturn(false);

        SeatReservationRejectedConsumerService.Result result =
                service.handle(context.bookingId().toString(), context.message());

        assertThat(result.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.DUPLICATE);

        verifyNoInteractions(bookingRepository);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void bookingNotFoundShouldThrowNotFoundException() {

        TestContext context = validContext();

        prepareUntilBookingLookup(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void showtimeMismatchShouldThrowConflictException() {

        TestContext context = validContext();

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        context.bookingId(),
                        UuidGenerator.next(),
                        "SEAT_UNAVAILABLE",
                        "Seat unavailable",
                        List.of("H7"),
                        NOW);

        when(payloadReader.read(context.message())).thenReturn(payload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingRepository, never()).save(context.booking());
    }

    @Test
    void unsupportedReasonShouldBeRejectedBeforeRegistration() {

        TestContext context = validContext();

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        "DATABASE_FAILURE",
                        "Internal database failure",
                        List.of(),
                        NOW);

        when(payloadReader.read(context.message())).thenReturn(payload);

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRegistrationService, bookingRepository);
    }

    @Test
    void aggregateIdMismatchShouldBeRejectedBeforeRegistration() {

        TestContext context = validContext();

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        UuidGenerator.next(),
                        context.showtimeId(),
                        "SEAT_UNAVAILABLE",
                        "Seat unavailable",
                        List.of("H7"),
                        NOW);

        when(payloadReader.read(context.message())).thenReturn(payload);

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRegistrationService, bookingRepository);
    }

    @Test
    void duplicateUnavailableSeatsShouldBeRejected() {

        TestContext context = validContext();

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        "SEAT_UNAVAILABLE",
                        "Seat unavailable",
                        List.of("H7", " h7 "),
                        NOW);

        when(payloadReader.read(context.message())).thenReturn(payload);

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(processedEventRegistrationService, bookingRepository);
    }

    @Test
    void nonPendingBookingShouldRejectEvent() {

        TestContext context = validContext();

        context.booking().reject("INVALID_REQUEST");

        prepareSuccessfulProcessing(context);

        assertThatThrownBy(() -> service.handle(context.bookingId().toString(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingRepository, never()).save(context.booking());
    }

    private void prepareSuccessfulProcessing(TestContext context) {

        prepareUntilBookingLookup(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));
    }

    private void prepareUntilBookingLookup(TestContext context) {

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        prepareRegistration(context);
    }

    private void prepareRegistration(TestContext context) {

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        "booking-seat-rejected",
                        "seat-reservation-rejected",
                        "1"))
                .thenReturn(true);
    }

    private TestContext validContext() {

        UUID bookingId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        showtimeId,
                        "rejected-result-test",
                        "a".repeat(64),
                        EXPIRES_AT,
                        NOW);

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        bookingId,
                        showtimeId,
                        "SEAT_UNAVAILABLE",
                        "One or more requested seats are unavailable",
                        List.of("H7"),
                        NOW);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        "BOOKING",
                        "seat-reservation-rejected",
                        "1",
                        NOW,
                        "inventory-service",
                        UuidGenerator.next(),
                        null,
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(bookingId, showtimeId, booking, payload, message);
    }

    private record TestContext(
            UUID bookingId,
            UUID showtimeId,
            Booking booking,
            SeatReservationRejectedPayload payload,
            OutboxEventMessage message) {}
}
