package com.cinema.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.service.OutboxService;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class PaymentRequestedPublicationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime HELD_AT = OffsetDateTime.parse("2026-08-25T10:00:00Z");

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.parse("2026-08-25T10:10:00Z");

    @Autowired private SeatReservedConsumerService consumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @MockitoSpyBean private OutboxService outboxService;

    @BeforeEach
    void setUp() {

        reset(outboxService);

        cleanDatabase();
    }

    @AfterEach
    void cleanDatabaseAfterTest() {

        reset(outboxService);

        cleanDatabase();
    }

    @Test
    void seatReservedShouldAtomicallyCreatePaymentRequestedEvent() throws Exception {

        TestContext context = createPendingBooking();

        OutboxEventMessage sourceEvent = seatReservedMessage(context);

        SeatReservedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), sourceEvent);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(result.status()).isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.getTotalAmount()).isEqualByComparingTo("180000.00");

        assertThat(booking.getCurrency()).isEqualTo("VND");

        assertThat(bookingSeats)
                .singleElement()
                .satisfies(
                        bookingSeat -> {
                            assertThat(bookingSeat.hasCompletedSnapshot()).isTrue();

                            assertThat(bookingSeat.getInventorySeatId())
                                    .isEqualTo(context.inventorySeatId());

                            assertThat(bookingSeat.getPrice()).isEqualByComparingTo("180000.00");
                        });

        assertThat(processedEventRepository.count()).isEqualTo(1);

        List<OutboxEventEntity> paymentEvents = paymentRequestedEvents();

        assertThat(paymentEvents).hasSize(1);

        OutboxEventEntity paymentEvent = paymentEvents.getFirst();

        assertThat(paymentEvent.getAggregateId()).isEqualTo(context.bookingId());

        assertThat(paymentEvent.getPartitionKey()).isEqualTo(context.bookingId().toString());

        assertThat(paymentEvent.getCorrelationId()).isEqualTo(sourceEvent.correlationId());

        assertThat(paymentEvent.getCausationId()).isEqualTo(sourceEvent.eventId());

        assertThat(paymentEvent.getOccurredAt()).isNotNull();

        JsonNode payload = objectMapper.readTree(paymentEvent.getPayload());

        assertThat(payload.get("bookingId").asText()).isEqualTo(context.bookingId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(context.userId().toString());

        assertThat(payload.get("amount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("180000.00"));

        assertThat(payload.get("currency").asText()).isEqualTo("VND");

        assertThat(payload.get("paymentAttempt").asInt()).isEqualTo(1);

        assertThat(OffsetDateTime.parse(payload.get("holdExpiresAt").asText()))
                .isEqualTo(HOLD_EXPIRES_AT);

        assertThat(OffsetDateTime.parse(payload.get("requestedAt").asText()))
                .isEqualTo(paymentEvent.getOccurredAt());
    }

    @Test
    void duplicateSeatReservedShouldNotCreateAnotherPaymentRequest() {

        TestContext context = createPendingBooking();

        OutboxEventMessage sourceEvent = seatReservedMessage(context);

        SeatReservedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), sourceEvent);

        SeatReservedConsumerService.Result second =
                consumerService.handle(context.bookingId().toString(), sourceEvent);

        assertThat(first.status()).isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        assertThat(second.status()).isEqualTo(SeatReservedConsumerService.Status.DUPLICATE);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(paymentRequestedEvents()).hasSize(1);
    }

    @Test
    void outboxFailureShouldRollbackReservationAndProcessedMarker() {

        TestContext context = createPendingBooking();

        OutboxEventMessage sourceEvent = seatReservedMessage(context);

        doThrow(new IllegalStateException("Simulated payment Outbox failure"))
                .when(outboxService)
                .save(any(OutboxEventEntity.class));

        assertThatThrownBy(
                        () -> consumerService.handle(context.bookingId().toString(), sourceEvent))
                .isInstanceOf(IllegalStateException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        List<BookingSeat> bookingSeats =
                bookingSeatRepository.findAllByBookingIdOrderBySeatNumberAsc(context.bookingId());

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);

        assertThat(booking.getTotalAmount()).isNull();

        assertThat(booking.getCurrency()).isNull();

        assertThat(bookingSeats)
                .singleElement()
                .satisfies(
                        bookingSeat -> {
                            assertThat(bookingSeat.hasCompletedSnapshot()).isFalse();

                            assertThat(bookingSeat.getInventorySeatId()).isNull();

                            assertThat(bookingSeat.getPrice()).isNull();
                        });

        assertThat(processedEventRepository.count()).isZero();

        assertThat(paymentRequestedEvents()).isEmpty();
    }

    private TestContext createPendingBooking() {

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID inventorySeatId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-publication-" + UuidGenerator.next(),
                        "a".repeat(64),
                        HOLD_EXPIRES_AT,
                        HELD_AT.minusMinutes(1));

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(savedBooking.getId(), showtimeId, "H7"));

        return new TestContext(savedBooking.getId(), userId, showtimeId, inventorySeatId);
    }

    private OutboxEventMessage seatReservedMessage(TestContext context) {

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        context.inventorySeatId(),
                                        "H7",
                                        "STANDARD",
                                        new BigDecimal("180000.00"))),
                        new BigDecimal("180000.00"),
                        "VND",
                        HELD_AT,
                        HOLD_EXPIRES_AT);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                BookingEventContract.SEAT_RESERVED,
                BookingEventContract.SEAT_RESERVED_VERSION,
                HELD_AT,
                BookingEventContract.INVENTORY_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private List<OutboxEventEntity> paymentRequestedEvents() {

        return outboxRepository.findAll().stream()
                .filter(
                        event ->
                                BookingEventContract.PAYMENT_REQUESTED.equals(event.getEventType()))
                .toList();
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }

    private record TestContext(
            UUID bookingId, UUID userId, UUID showtimeId, UUID inventorySeatId) {}
}
