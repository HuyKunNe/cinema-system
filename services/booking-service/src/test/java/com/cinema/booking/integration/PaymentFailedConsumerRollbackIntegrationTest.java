package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.SeatReleaseRequestedOutboxFactory;
import com.cinema.booking.event.payload.PaymentFailedPayload;
import com.cinema.booking.exception.BookingErrorCode;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.PaymentFailedConsumerService;
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

class PaymentFailedConsumerRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("90000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("210000.00");

    private static final String CURRENCY = "VND";

    @Autowired private PaymentFailedConsumerService consumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @MockitoBean private SeatReleaseRequestedOutboxFactory seatReleaseRequestedOutboxFactory;

    @BeforeEach
    void cleanDatabaseBeforeTest() {

        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        cleanDatabase();
    }

    @Test
    void seatReleaseFactoryFailureShouldRollbackBookingAndProcessedMarker() {

        TestContext context = persistReservedBooking();

        OutboxEventMessage message =
                paymentFailedMessage(context, UuidGenerator.next(), UuidGenerator.next());

        when(seatReleaseRequestedOutboxFactory.create(
                        any(Booking.class), anyList(), eq(message), any(OffsetDateTime.class)))
                .thenThrow(
                        new InternalServerException(
                                BookingErrorCode.OUTBOX_PAYLOAD_SERIALIZATION_FAILED));

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(InternalServerException.class);

        verify(seatReleaseRequestedOutboxFactory)
                .create(any(Booking.class), anyList(), eq(message), any(OffsetDateTime.class));

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        /*
         * Aggregate transition must roll back.
         */
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getRejectionReason()).isNull();

        assertThat(booking.getConfirmedAt()).isNull();

        assertThat(booking.getCancelledAt()).isNull();

        assertThat(booking.getTotalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(booking.getCurrency()).isEqualTo(CURRENCY);

        /*
         * Processed marker was inserted before the aggregate lock,
         * but belongs to the same transaction and must roll back.
         */
        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                message.eventId(), BookingEventContract.PAYMENT_FAILED_CONSUMER))
                .isFalse();

        assertThat(processedEventRepository.count()).isZero();

        /*
         * No compensation command may survive a failed transaction.
         */
        assertThat(outboxRepository.count()).isZero();

        List<BookingSeat> seats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(seats)
                .hasSize(2)
                .allSatisfy(seat -> assertThat(seat.hasCompletedSnapshot()).isTrue());

        assertThat(seats.get(0).getSeatNumber()).isEqualTo("H7");

        assertThat(seats.get(0).getInventorySeatId()).isEqualTo(context.firstInventorySeatId());

        assertThat(seats.get(0).getSeatType()).isEqualTo("STANDARD");

        assertThat(seats.get(0).getPrice()).isEqualByComparingTo(FIRST_SEAT_PRICE);

        assertThat(seats.get(1).getSeatNumber()).isEqualTo("H8");

        assertThat(seats.get(1).getInventorySeatId()).isEqualTo(context.secondInventorySeatId());

        assertThat(seats.get(1).getSeatType()).isEqualTo("VIP");

        assertThat(seats.get(1).getPrice()).isEqualByComparingTo(SECOND_SEAT_PRICE);
    }

    private TestContext persistReservedBooking() {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-failed-rollback-" + UuidGenerator.next(),
                        "a".repeat(64),
                        now.plusMinutes(30),
                        now.minusMinutes(1));

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        UUID firstInventorySeatId = UuidGenerator.next();

        BookingSeat firstSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H7");

        firstSeat.completeSnapshot(firstInventorySeatId, "STANDARD", FIRST_SEAT_PRICE);

        UUID secondInventorySeatId = UuidGenerator.next();

        BookingSeat secondSeat = new BookingSeat(savedBooking.getId(), showtimeId, "H8");

        secondSeat.completeSnapshot(secondInventorySeatId, "VIP", SECOND_SEAT_PRICE);

        bookingSeatRepository.saveAllAndFlush(List.of(firstSeat, secondSeat));

        entityManager.clear();

        return new TestContext(savedBooking.getId(), firstInventorySeatId, secondInventorySeatId);
    }

    private OutboxEventMessage paymentFailedMessage(
            TestContext context, UUID eventId, UUID paymentId) {

        OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);

        PaymentFailedPayload payload =
                new PaymentFailedPayload(
                        paymentId,
                        context.bookingId(),
                        "PAYMENT_DECLINED",
                        "The payment was declined",
                        failedAt,
                        false);

        return new OutboxEventMessage(
                eventId,
                paymentId,
                BookingEventContract.PAYMENT_AGGREGATE_TYPE,
                BookingEventContract.PAYMENT_FAILED,
                BookingEventContract.PAYMENT_FAILED_VERSION,
                failedAt,
                BookingEventContract.PAYMENT_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void cleanDatabase() {

        outboxRepository.deleteAllInBatch();

        processedEventRepository.deleteAllInBatch();

        bookingSeatRepository.deleteAllInBatch();

        bookingRepository.deleteAllInBatch();
    }

    private record TestContext(
            UUID bookingId, UUID firstInventorySeatId, UUID secondInventorySeatId) {}
}
