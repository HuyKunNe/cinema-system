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
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.SeatReleaseRequestedOutboxFactory;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.event.serialization.PaymentFailedPayloadReader;
import com.cinema.booking.event.validation.PaymentResultMessageValidator;
import com.cinema.booking.event.validation.PaymentResultPayloadValidator;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.PaymentFailedConsumerService;
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
class PaymentFailedConsumerServiceImplTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-15T09:50:00Z");

    private static final OffsetDateTime FAILED_AT = OffsetDateTime.parse("2026-09-15T09:59:00Z");

    private static final OffsetDateTime REQUESTED_AT = OffsetDateTime.parse("2026-09-15T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-09-15T10:10:00Z");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    private static final String FAILURE_CODE = "PAYMENT_DECLINED";

    private static final String FAILURE_MESSAGE = "The payment was declined";

    @Mock private PaymentResultMessageValidator messageValidator;

    @Mock private PaymentFailedPayloadReader payloadReader;

    @Mock private PaymentResultPayloadValidator payloadValidator;

    @Mock private ProcessedEventRegistrationService processedEventRegistrationService;

    @Mock private BookingRepository bookingRepository;

    @Mock private BookingSeatRepository bookingSeatRepository;

    @Mock private SeatReleaseRequestedOutboxFactory seatReleaseRequestedOutboxFactory;

    @Mock private OutboxService outboxService;

    private PaymentFailedConsumerServiceImpl service;

    @BeforeEach
    void setUp() {

        service =
                new PaymentFailedConsumerServiceImpl(
                        messageValidator,
                        payloadReader,
                        payloadValidator,
                        processedEventRegistrationService,
                        bookingRepository,
                        bookingSeatRepository,
                        seatReleaseRequestedOutboxFactory,
                        outboxService,
                        Clock.fixed(REQUESTED_AT.toInstant(), ZoneOffset.UTC));
    }

    @Test
    void validEventShouldFailBookingAndCreateSeatReleaseOutbox() {

        TestContext context = validContext();

        OutboxEventEntity seatReleaseEvent = mock(OutboxEventEntity.class);

        prepareSuccessfulProcessing(context);

        when(seatReleaseRequestedOutboxFactory.create(
                        context.booking(), context.seats(), context.message(), REQUESTED_AT))
                .thenReturn(seatReleaseEvent);

        PaymentFailedConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(PaymentFailedConsumerService.Status.PAYMENT_FAILED);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(context.booking().getRejectionReason()).isEqualTo(FAILURE_CODE);

        /*
         * Provider-facing message is deliberately not persisted
         * in the Booking aggregate.
         */
        assertThat(context.booking().getRejectionReason())
                .isNotEqualTo(context.payload().message());

        assertThat(context.booking().getConfirmedAt()).isNull();

        assertThat(context.booking().getCancelledAt()).isNull();

        verify(messageValidator).validateFailed(context.partitionKey(), context.message());

        verify(payloadReader).read(context.message());

        verify(payloadValidator)
                .validateFailed(context.partitionKey(), context.message(), context.payload());

        verify(processedEventRegistrationService)
                .register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_FAILED_CONSUMER,
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION);

        verify(bookingRepository).findByIdForUpdate(context.bookingId());

        verify(bookingSeatRepository).findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        verify(bookingRepository).save(context.booking());

        verify(seatReleaseRequestedOutboxFactory)
                .create(context.booking(), context.seats(), context.message(), REQUESTED_AT);

        verify(outboxService).save(seatReleaseEvent);
    }

    @Test
    void duplicateEventShouldNotLoadOrModifyBooking() {

        TestContext context = validContext();

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_FAILED_CONSUMER,
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION))
                .thenReturn(false);

        PaymentFailedConsumerService.Result result =
                service.handle(context.partitionKey(), context.message());

        assertThat(result.status()).isEqualTo(PaymentFailedConsumerService.Status.DUPLICATE);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(context.booking().getRejectionReason()).isNull();

        verify(messageValidator).validateFailed(context.partitionKey(), context.message());

        verify(payloadValidator)
                .validateFailed(context.partitionKey(), context.message(), context.payload());

        verifyNoInteractions(
                bookingRepository,
                bookingSeatRepository,
                seatReleaseRequestedOutboxFactory,
                outboxService);
    }

    @Test
    void payloadValidationFailureShouldRejectBeforeRegistration() {

        TestContext context = validContext();

        ValidationException failure =
                new ValidationException(
                        com.cinema.booking.exception.BookingErrorCode.EVENT_PAYLOAD_INVALID);

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        doThrow(failure)
                .when(payloadValidator)
                .validateFailed(context.partitionKey(), context.message(), context.payload());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verifyNoInteractions(
                processedEventRegistrationService,
                bookingRepository,
                bookingSeatRepository,
                seatReleaseRequestedOutboxFactory,
                outboxService);
    }

    @Test
    void unsupportedFailureCodeShouldRejectBeforeRegistration() {

        TestContext context = validContext();

        PaymentFailedPayload unsupportedPayload =
                new PaymentFailedPayload(
                        context.paymentId(),
                        context.bookingId(),
                        "DATABASE_CONNECTION_FAILED",
                        "Internal database failure",
                        FAILED_AT,
                        false);

        when(payloadReader.read(context.message())).thenReturn(unsupportedPayload);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(
                processedEventRegistrationService,
                bookingRepository,
                bookingSeatRepository,
                seatReleaseRequestedOutboxFactory,
                outboxService);
    }

    @Test
    void bookingNotFoundShouldThrowNotFoundException() {

        TestContext context = validContext();

        prepareRegistration(context);

        when(bookingRepository.findByIdForUpdate(context.bookingId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(
                bookingSeatRepository, seatReleaseRequestedOutboxFactory, outboxService);
    }

    @Test
    void confirmedBookingShouldNotBeChangedToPaymentFailed() {

        TestContext context = validContext();

        context.booking().confirm(FAILED_AT);

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(context.booking().getConfirmedAt()).isEqualTo(FAILED_AT);

        assertThat(context.booking().getRejectionReason()).isNull();

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(seatReleaseRequestedOutboxFactory, outboxService);
    }

    @Test
    void cancelledBookingShouldNotBeChangedToPaymentFailed() {

        TestContext context = validContext();

        context.booking().cancel(FAILED_AT);

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(context.booking().getCancelledAt()).isEqualTo(FAILED_AT);

        assertThat(context.booking().getRejectionReason()).isNull();

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(seatReleaseRequestedOutboxFactory, outboxService);
    }

    @Test
    void expiredBookingShouldNotBeChangedToPaymentFailed() {

        TestContext context = expiredContext();

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertThat(context.booking().getRejectionReason()).isNull();

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(seatReleaseRequestedOutboxFactory, outboxService);
    }

    @Test
    void alreadyPaymentFailedBookingShouldNotCreateAnotherOutbox() {

        TestContext context = validContext();

        context.booking().failPayment(FAILURE_CODE);

        prepareUntilTransition(context);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isInstanceOf(ConflictException.class);

        assertThat(context.booking().getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);

        assertThat(context.booking().getRejectionReason()).isEqualTo(FAILURE_CODE);

        verify(bookingRepository, never()).save(context.booking());

        verifyNoInteractions(seatReleaseRequestedOutboxFactory, outboxService);
    }

    @Test
    void seatReleaseOutboxFactoryFailureShouldPropagate() {

        TestContext context = validContext();

        prepareSuccessfulProcessing(context);

        IllegalStateException failure =
                new IllegalStateException("Simulated seat-release-requested factory failure");

        when(seatReleaseRequestedOutboxFactory.create(
                        context.booking(), context.seats(), context.message(), REQUESTED_AT))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verify(bookingRepository).save(context.booking());

        verify(seatReleaseRequestedOutboxFactory)
                .create(context.booking(), context.seats(), context.message(), REQUESTED_AT);

        verifyNoInteractions(outboxService);
    }

    @Test
    void seatReleaseOutboxPersistenceFailureShouldPropagate() {

        TestContext context = validContext();

        OutboxEventEntity seatReleaseEvent = mock(OutboxEventEntity.class);

        prepareSuccessfulProcessing(context);

        when(seatReleaseRequestedOutboxFactory.create(
                        context.booking(), context.seats(), context.message(), REQUESTED_AT))
                .thenReturn(seatReleaseEvent);

        IllegalStateException failure =
                new IllegalStateException("Simulated seat-release-requested persistence failure");

        doThrow(failure).when(outboxService).save(seatReleaseEvent);

        assertThatThrownBy(() -> service.handle(context.partitionKey(), context.message()))
                .isSameAs(failure);

        verify(bookingRepository).save(context.booking());

        verify(seatReleaseRequestedOutboxFactory)
                .create(context.booking(), context.seats(), context.message(), REQUESTED_AT);

        verify(outboxService).save(seatReleaseEvent);
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

        when(payloadReader.read(context.message())).thenReturn(context.payload());

        when(processedEventRegistrationService.register(
                        context.message().eventId(),
                        BookingEventContract.PAYMENT_FAILED_CONSUMER,
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION))
                .thenReturn(true);
    }

    private TestContext validContext() {

        Booking booking = reservedBooking(EXPIRES_AT);

        return contextFor(booking);
    }

    private TestContext expiredContext() {

        OffsetDateTime expiredAt = REQUESTED_AT.minusMinutes(1);

        Booking booking = reservedBooking(expiredAt);

        booking.expire(REQUESTED_AT);

        return contextFor(booking);
    }

    private Booking reservedBooking(OffsetDateTime expiresAt) {

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        "payment-failed-consumer-test-" + UuidGenerator.next(),
                        "a".repeat(64),
                        expiresAt,
                        CREATED_AT);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        return booking;
    }

    private TestContext contextFor(Booking booking) {

        UUID bookingId = booking.getId();

        UUID paymentId = UuidGenerator.next();

        BookingSeat firstSeat = completedSeat(booking, "H7", "STANDARD", "90000.00");

        BookingSeat secondSeat = completedSeat(booking, "H8", "VIP", "120000.00");

        List<BookingSeat> seats = List.of(firstSeat, secondSeat);

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        paymentId, bookingId, FAILURE_CODE, FAILURE_MESSAGE, FAILED_AT, false);

        OutboxEventMessage message =
                new OutboxEventMessage(
                        UuidGenerator.next(),
                        paymentId,
                        BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                        BookingEventContract.PAYMENT_FAILED,
                        BookingEventContract.PAYMENT_FAILED_VERSION,
                        FAILED_AT,
                        BookingEventContract.PAYMENT_PRODUCER,
                        UuidGenerator.next(),
                        UuidGenerator.next(),
                        JsonNodeFactory.instance.objectNode());

        return new TestContext(
                bookingId.toString(), bookingId, paymentId, booking, seats, payload, message);
    }

    private BookingSeat completedSeat(
            Booking booking, String seatNumber, String seatType, String price) {

        BookingSeat seat = new BookingSeat(booking.getId(), booking.getShowtimeId(), seatNumber);

        seat.completeSnapshot(UuidGenerator.next(), seatType, new BigDecimal(price));

        return seat;
    }

    private record TestContext(
            String partitionKey,
            UUID bookingId,
            UUID paymentId,
            Booking booking,
            List<BookingSeat> seats,
            PaymentFailedPayload payload,
            OutboxEventMessage message) {}
}
