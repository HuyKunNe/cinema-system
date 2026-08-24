package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.event.serialization.SeatReservedPayloadReader;
import com.cinema.booking.event.validation.SeatReservedMessageValidator;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.model.OutboxEventMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class SeatReservedConsumerServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    private static final String PARTITION_KEY_PREFIX = "";

    @Mock private SeatReservedMessageValidator messageValidator;

    @Mock private SeatReservedPayloadReader payloadReader;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    private SeatReservedConsumerServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new SeatReservedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        processedEventRegistrationService,
                        bookingRepository,
                        bookingSeatRepository);
    }

    @Test
    void validEventShouldCompleteSnapshotsAndReserveBooking() {

        TestContext context = validContext();

        prepareSuccessfulProcessing(context);

        SeatReservedConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(context.booking().getTotalAmount()).isEqualByComparingTo("210000.00");

        assertThat(context.booking().getCurrency()).isEqualTo("VND");

        assertThat(context.bookingSeats())
                .allSatisfy(bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isTrue());

        BookingSeat h7 = context.bookingSeats().get(0);

        BookingSeat h8 = context.bookingSeats().get(1);

        assertThat(h7.getSeatNumber()).isEqualTo("H7");

        assertThat(h7.getInventorySeatId()).isEqualTo(context.h7InventorySeatId());

        assertThat(h7.getSeatType()).isEqualTo("STANDARD");

        assertThat(h7.getPrice()).isEqualByComparingTo("90000.00");

        assertThat(h8.getSeatNumber()).isEqualTo("H8");

        assertThat(h8.getInventorySeatId()).isEqualTo(context.h8InventorySeatId());

        assertThat(h8.getSeatType()).isEqualTo("VIP");

        assertThat(h8.getPrice()).isEqualByComparingTo("120000.00");

        verify(bookingSeatRepository).saveAll(context.bookingSeats());

        verify(bookingRepository).save(context.booking());
    }

    @Test
    void duplicateEventShouldNotLoadOrModifyBooking() {

        TestContext context = validContext();

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(), "booking-seat-reserved", "seat-reserved", "1"))
                .thenReturn(false);

        SeatReservedConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(SeatReservedConsumerService.Status.DUPLICATE);

        verifyNoInteractions(bookingRepository, bookingSeatRepository);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(context.bookingSeats())
                .allSatisfy(
                        bookingSeat -> assertThat(bookingSeat.hasCompletedSnapshot()).isFalse());
    }

    @Test
    void bookingNotFoundShouldThrowNotFoundException() {

        TestContext context = validContext();

        prepareUntilBookingLookup(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(bookingSeatRepository);
    }

    @Test
    void showtimeMismatchShouldRejectEvent() {

        TestContext context = validContext();

        SeatReservedPayload mismatchedPayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        UuidGenerator.next(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().heldAt(),
                        context.payload().holdExpiresAt());

        when(payloadReader.read(context.message())).thenReturn(mismatchedPayload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(bookingSeatRepository);

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void expirationMismatchShouldRejectEvent() {

        TestContext context = validContext();

        SeatReservedPayload mismatchedPayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().heldAt(),
                        EXPIRES_AT.plusMinutes(1));

        when(payloadReader.read(context.message())).thenReturn(mismatchedPayload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verifyNoInteractions(bookingSeatRepository);
    }

    @Test
    void seatCountMismatchShouldRejectEvent() {

        TestContext context = validContext();

        SeatReservedPayload incompletePayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(context.payload().seats().get(0)),
                        new BigDecimal("90000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        prepareProcessing(context, incompletePayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingSeatRepository, never()).saveAll(any());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void seatNumberMismatchShouldRejectEvent() {

        TestContext context = validContext();

        ReservedSeatPayload wrongSeat =
                new ReservedSeatPayload(
                        UuidGenerator.next(), "H9", "STANDARD", new BigDecimal("90000.00"));

        SeatReservedPayload mismatchedPayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(wrongSeat, context.payload().seats().get(1)),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        prepareProcessing(context, mismatchedPayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingSeatRepository, never()).saveAll(any());
    }

    @Test
    void duplicateSeatNumberShouldRejectEvent() {

        TestContext context = validContext();

        SeatReservedPayload duplicatePayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        UuidGenerator.next(),
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("90000.00")),
                                new ReservedSeatPayload(
                                        UuidGenerator.next(),
                                        " h7 ",
                                        "VIP",
                                        new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        when(payloadReader.read(context.message())).thenReturn(duplicatePayload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId()))
                .thenReturn(context.bookingSeats());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ValidationException.class);

        verify(bookingSeatRepository, never()).saveAll(any());
    }

    @Test
    void duplicateInventorySeatIdShouldRejectEvent() {

        TestContext context = validContext();

        UUID inventorySeatId = UuidGenerator.next();

        SeatReservedPayload duplicatePayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        inventorySeatId,
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("90000.00")),
                                new ReservedSeatPayload(
                                        inventorySeatId, "H8", "VIP", new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        when(payloadReader.read(context.message())).thenReturn(duplicatePayload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId()))
                .thenReturn(context.bookingSeats());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ValidationException.class);

        verify(bookingSeatRepository, never()).saveAll(any());
    }

    @Test
    void totalMismatchShouldRejectEvent() {

        TestContext context = validContext();

        SeatReservedPayload mismatchedPayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        context.payload().seats(),
                        new BigDecimal("200000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        prepareProcessing(context, mismatchedPayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingSeatRepository, never()).saveAll(any());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void nonPendingBookingShouldRejectEvent() {

        TestContext context = validContext();

        context.booking().reject("SEAT_UNAVAILABLE");

        prepareSuccessfulProcessing(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        verify(bookingSeatRepository, never()).saveAll(any());

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void aggregateIdMismatchShouldRejectBeforeRegistration() {

        TestContext context = validContext();

        SeatReservedPayload mismatchedPayload =
                new SeatReservedPayload(
                        UuidGenerator.next(),
                        context.showtimeId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        context.payload().heldAt(),
                        context.payload().holdExpiresAt());

        when(payloadReader.read(context.message())).thenReturn(mismatchedPayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
                processedEventRegistrationService, bookingRepository, bookingSeatRepository);
    }

    @Test
    void invalidHoldPeriodShouldRejectBeforeRegistration() {

        TestContext context = validContext();

        SeatReservedPayload invalidPayload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        context.payload().seats(),
                        context.payload().totalAmount(),
                        context.payload().currency(),
                        EXPIRES_AT,
                        NOW);

        when(payloadReader.read(context.message())).thenReturn(invalidPayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
                processedEventRegistrationService, bookingRepository, bookingSeatRepository);
    }

    private void prepareSuccessfulProcessing(TestContext context) {

        prepareProcessing(context, context.payload());
    }

    private void prepareProcessing(TestContext context, SeatReservedPayload payload) {

        when(payloadReader.read(context.message())).thenReturn(payload);

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId()))
                .thenReturn(context.bookingSeats());
    }

    private void prepareUntilBookingLookup(TestContext context) {

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        prepareRegistration(context);
    }

    private void prepareRegistration(TestContext context) {

        when(processedEventRegistrationService.register(
                        context.message().eventId(), "booking-seat-reserved", "seat-reserved", "1"))
                .thenReturn(true);
    }

    private TestContext validContext() {

        UUID bookingId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID h7InventorySeatId = UuidGenerator.next();

        UUID h8InventorySeatId = UuidGenerator.next();

        Booking booking =
                new Booking(userId, showtimeId, "request-001", "a".repeat(64), EXPIRES_AT, NOW);

        List<BookingSeat> bookingSeats =
                List.of(
                        new BookingSeat(bookingId, showtimeId, "H7"),
                        new BookingSeat(bookingId, showtimeId, "H8"));

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        bookingId,
                        showtimeId,
                        List.of(
                                new ReservedSeatPayload(
                                        h7InventorySeatId,
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("90000.00")),
                                new ReservedSeatPayload(
                                        h8InventorySeatId,
                                        "H8",
                                        "VIP",
                                        new BigDecimal("120000.00"))),
                        new BigDecimal("210000.00"),
                        "VND",
                        NOW,
                        EXPIRES_AT);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        bookingId,
                        "BOOKING",
                        "seat-reserved",
                        "1",
                        NOW,
                        "inventory-service",
                        UuidGenerator.next(),
                        null,
                        null);

        return new TestContext(
                bookingId,
                showtimeId,
                h7InventorySeatId,
                h8InventorySeatId,
                booking,
                bookingSeats,
                payload,
                message);
    }

    private record TestContext(
            UUID bookingId,
            UUID showtimeId,
            UUID h7InventorySeatId,
            UUID h8InventorySeatId,
            Booking booking,
            List<BookingSeat> bookingSeats,
            SeatReservedPayload payload,
            OutboxEventMessage message) {

        String partitionKey() {

            return PARTITION_KEY_PREFIX + bookingId;
        }
    }
}
