package com.cinema.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingConfirmedOutboxFactory;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.event.serialization.PaymentSucceededPayloadReader;
import com.cinema.booking.event.validation.PaymentResultMessageValidator;
import com.cinema.booking.event.validation.PaymentResultPayloadValidator;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.PaymentSucceededConsumerService;
import com.cinema.booking.service.ProcessedEventRegistrationService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentSucceededConsumerServiceImplTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-15T09:50:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-09-15T10:10:00Z");

    private static final OffsetDateTime PAID_AT = OffsetDateTime.parse("2026-09-15T09:59:00Z");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    @Mock private PaymentResultMessageValidator messageValidator;

    @Mock private PaymentSucceededPayloadReader payloadReader;

    @Mock private PaymentResultPayloadValidator payloadValidator;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private BookingConfirmedOutboxFactory bookingConfirmedOutboxFactory;

    @Mock private OutboxService outboxService;

    private PaymentSucceededConsumerServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new PaymentSucceededConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        bookingRepository,
                        bookingSeatRepository,
                        bookingConfirmedOutboxFactory,
                        outboxService,
                        Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }

    @Test
    void validPaymentSucceededShouldConfirmBookingAndCreateOutbox() {

        TestContext context = validContext();

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        prepareSuccessfulProcessing(context);

        when(bookingConfirmedOutboxFactory.create(
                        context.booking(), context.seats(), context.paymentId(), context.message()))
                .thenReturn(outboxEvent);

        PaymentSucceededConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(PaymentSucceededConsumerService.Status.CONFIRMED);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(context.booking().getConfirmedAt()).isEqualTo(NOW);

        verify(messageValidator).validateSucceeded(context.partitionKey(), context.message());

        verify(payloadReader).read(context.message());

        verify(payloadValidator)
                .validateSucceeded(context.partitionKey(), context.message(), context.payload());

        verify(processedEventRegistrationService)
                .register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER,
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION);

        verify(bookingRepository).findByIdForUpdate(context.bookingId());

        verify(bookingSeatRepository).findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        verify(bookingRepository).save(context.booking());

        verify(bookingConfirmedOutboxFactory)
                .create(context.booking(), context.seats(), context.paymentId(), context.message());

        verify(outboxService).save(outboxEvent);
    }

    @Test
    void duplicateEventShouldNotLoadOrModifyBooking() {

        TestContext context = validContext();

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER,
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION))
                .thenReturn(false);

        PaymentSucceededConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(PaymentSucceededConsumerService.Status.DUPLICATE);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        verify(messageValidator).validateSucceeded(context.partitionKey(), context.message());

        verify(payloadValidator)
                .validateSucceeded(context.partitionKey(), context.message(), context.payload());

        verifyNoInteractions(
                bookingRepository,
                bookingSeatRepository,
                bookingConfirmedOutboxFactory,
                outboxService);
    }

    @Test
    void invalidPayloadShouldRejectBeforeProcessedEventRegistration() {

        TestContext context = validContext();

        ValidationException failure =
                new ValidationException(BookingErrorCode.EVENT_PAYLOAD_INVALID);

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        doThrow(failure)
                .when(payloadValidator)
                .validateSucceeded(context.partitionKey(), context.message(), context.payload());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verifyNoInteractions(
                processedEventRegistrationService,
                bookingRepository,
                bookingSeatRepository,
                bookingConfirmedOutboxFactory,
                outboxService);
    }

    @Test
    void bookingNotFoundShouldThrowNotFoundException() {

        TestContext context = validContext();

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(bookingSeatRepository, bookingConfirmedOutboxFactory, outboxService);
    }

    @Test
    void paymentAmountMismatchShouldNotConfirmBookingOrCreateOutbox() {

        TestContext context = validContext();

        PaymentSucceededPayload mismatchedPayload =
                new PaymentSucceededPayload(
                        context.paymentId(),
                        context.bookingId(),
                        new BigDecimal("200000.00"),
                        "VND",
                        "MOMO",
                        "provider-reference",
                        PAID_AT);

        prepareRegistration(context, mismatchedPayload);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        verifyNoInteractions(bookingSeatRepository, bookingConfirmedOutboxFactory, outboxService);

        verify(bookingRepository, never()).save(context.booking());
    }

    @Test
    void paymentCurrencyMismatchShouldNotConfirmBookingOrCreateOutbox() {

        TestContext context = validContext();

        PaymentSucceededPayload mismatchedPayload =
                new PaymentSucceededPayload(
                        context.paymentId(),
                        context.bookingId(),
                        TOTAL_AMOUNT,
                        "USD",
                        "MOMO",
                        "provider-reference",
                        PAID_AT);

        prepareRegistration(context, mismatchedPayload);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        verifyNoInteractions(bookingSeatRepository, bookingConfirmedOutboxFactory, outboxService);

        verify(bookingRepository, never()).save(context.booking());
    }

    @Test
    void expiredReservationShouldNotBeConfirmed() {

        TestContext context = expiredContext();

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(context.booking().getConfirmedAt()).isNull();

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(bookingConfirmedOutboxFactory, outboxService);
    }

    @Test
    void alreadyConfirmedBookingShouldNotCreateAnotherOutbox() {

        TestContext context = validContext();

        context.booking().confirm(NOW.minusMinutes(1));

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(context.booking().getConfirmedAt()).isEqualTo(NOW.minusMinutes(1));

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(bookingConfirmedOutboxFactory, outboxService);
    }

    @Test
    void cancelledBookingShouldNotBeRestoredToConfirmed() {

        TestContext context = validContext();

        context.booking().cancel(NOW.minusMinutes(1));

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(context.booking().getConfirmedAt()).isNull();

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(bookingConfirmedOutboxFactory, outboxService);
    }

    @Test
    void outboxFactoryFailureShouldPropagate() {

        TestContext context = validContext();

        prepareSuccessfulProcessing(context);

        IllegalStateException failure =
                new IllegalStateException("Simulated booking-confirmed Outbox factory failure");

        when(bookingConfirmedOutboxFactory.create(
                        context.booking(), context.seats(), context.paymentId(), context.message()))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verify(bookingRepository).save(context.booking());

        verifyNoInteractions(outboxService);
    }

    @Test
    void outboxPersistenceFailureShouldPropagate() {

        TestContext context = validContext();

        OutboxEventEntity outboxEvent = mock(OutboxEventEntity.class);

        prepareSuccessfulProcessing(context);

        when(bookingConfirmedOutboxFactory.create(
                        context.booking(), context.seats(), context.paymentId(), context.message()))
                .thenReturn(outboxEvent);

        IllegalStateException failure =
                new IllegalStateException("Simulated booking-confirmed Outbox persistence failure");

        doThrow(failure).when(outboxService).save(outboxEvent);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verify(bookingRepository).save(context.booking());

        verify(bookingConfirmedOutboxFactory)
                .create(context.booking(), context.seats(), context.paymentId(), context.message());

        verify(outboxService).save(outboxEvent);
    }

    private void prepareSuccessfulProcessing(TestContext context) {

        prepareUntilTransition(context);

        when(bookingRepository.save(context.booking())).thenReturn(context.booking());
    }

    private void prepareUntilTransition(TestContext context) {

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId()))
                .thenReturn(Optional.of(context.booking()));

        when(bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId()))
                .thenReturn(context.seats());
    }

    private void prepareRegistration(TestContext context) {

        prepareRegistration(context, context.payload());
    }

    private void prepareRegistration(TestContext context, PaymentSucceededPayload payload) {

        when(payloadReader.read(context.message())).thenReturn(payload);

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER,
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION))
                .thenReturn(true);
    }

    private TestContext validContext() {

        return createContext(EXPIRES_AT);
    }

    private TestContext expiredContext() {

        return createContext(NOW);
    }

    private TestContext createContext(OffsetDateTime expiresAt) {

        UUID bookingId = UuidGenerator.next();

        UUID paymentId = UuidGenerator.next();

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-success-test",
                        "a".repeat(64),
                        expiresAt,
                        CREATED_AT);

        booking.reserve(TOTAL_AMOUNT, "VND");

        BookingSeat h7 = new BookingSeat(bookingId, showtimeId, "H7");

        h7.completeSnapshot(UuidGenerator.next(), "STANDARD", new BigDecimal("90000.00"));

        BookingSeat h8 = new BookingSeat(bookingId, showtimeId, "H8");

        h8.completeSnapshot(UuidGenerator.next(), "VIP", new BigDecimal("120000.00"));

        List<BookingSeat> seats = List.of(h7, h8);

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        paymentId,
                        bookingId,
                        TOTAL_AMOUNT,
                        "VND",
                        "MOMO",
                        "provider-reference",
                        PAID_AT);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        paymentId,
                        BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                        BookingEventContract.PAYMENT_SUCCEEDED,
                        BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                        PAID_AT,
                        BookingEventContract.PAYMENT_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(
                bookingId.toString(), bookingId, paymentId, booking, seats, payload, message);
    }

    private record TestContext(
            String partitionKey,
            UUID bookingId,
            UUID paymentId,
            Booking booking,
            List<BookingSeat> seats,
            PaymentSucceededPayload payload,
            OutboxEventMessage message) {}
}
