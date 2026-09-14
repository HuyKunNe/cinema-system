package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingConfirmedOutboxFactory;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.PaymentSucceededPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.PaymentSucceededConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class PaymentSucceededConsumerRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    @Autowired private PaymentSucceededConsumerService consumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @MockitoBean private BookingConfirmedOutboxFactory bookingConfirmedOutboxFactory;

    @BeforeEach
    void cleanDatabaseBeforeTest() {

        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        cleanDatabase();
    }

    @Test
    void bookingConfirmedFactoryFailureShouldRollbackBookingAndProcessedMarker() {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentSucceededMessage(context, UuidGenerator.next(), UuidGenerator.next());

        when(bookingConfirmedOutboxFactory.create(
                        any(Booking.class), anyList(), eq(message.aggregateId()), eq(message)))
                .thenThrow(
                        new InternalServerException(
                                BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED));

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(InternalServerException.class);

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(reloaded.getConfirmedAt()).isNull();

        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(reloaded.getCurrency()).isEqualTo(CURRENCY);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_SUCCEEDED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        assertThat(outboxRepository.count()).isZero();
    }

    private TestContext persistReservedBooking() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-success-rollback-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(UuidGenerator.next(), "STANDARD", FIRST_SEAT_PRICE);

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(UuidGenerator.next(), "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(savedBooking.getId(), userId, showtimeId);
    }

    private OutboxEventMessage paymentSucceededMessage(
            TestContext context, UUID eventId, UUID paymentId) {

        OffsetDateTime paidAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentSucceededPayload payload =
                new PaymentSucceededPayload(
                        paymentId,
                        context.bookingId(),
                        TOTAL_AMOUNT,
                        CURRENCY,
                        "MOMO",
                        "momo-" + paymentId,
                        paidAt);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_SUCCEEDED,
                BookingEventContract.PAYMENT_SUCCEEDED_VERSION,
                paidAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }

    private record TestContext(UUID bookingId, UUID userId, UUID showtimeId) {}
}
