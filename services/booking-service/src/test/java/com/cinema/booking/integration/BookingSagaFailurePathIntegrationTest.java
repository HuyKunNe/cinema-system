package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.BookingCancellationService;
import com.cinema.booking.service.BookingExpirationService;
import com.cinema.booking.service.SeatReservationRejectedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

class BookingSagaFailurePathIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("250000.00");

    private static final String CURRENCY = "VND";

    @Autowired
    private SeatReservationRejectedConsumerService seatReservationRejectedConsumerService;

    @Autowired private BookingCancellationService bookingCancellationService;

    @Autowired private BookingExpirationService bookingExpirationService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {

        cleanDatabase();
    }

    @Test
    void seatReservationRejectedShouldNotCreatePaymentRequestedEvent() {

        OffsetDateTime now = currentTime();

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Booking booking = persistPendingBooking(userId, showtimeId, now, now.plusMinutes(10));

        UUID eventId = UuidGenerator.next();

        OutboxEventMessage message =
                seatReservationRejectedMessage(booking, showtimeId, eventId, now);

        SeatReservationRejectedConsumerService.Result result =
                seatReservationRejectedConsumerService.handle(booking.getId().toString(), message);

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(result.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.REJECTED);

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(reloaded.getRejectionReason()).isEqualTo("SEAT_UNAVAILABLE");

        assertThat(reloaded.getTotalAmount()).isNull();

        assertThat(reloaded.getCurrency()).isNull();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(
                        processedEventRepository.existsByEventIdAndConsumerName(
                                eventId, BookingEventContract.SEAT_RESERVATION_REJECTED_CONSUMER))
                .isTrue();

        assertThat(outboxRepository.findAll()).isEmpty();

        assertNoPaymentRequestedEvent();
    }

    @Test
    void cancellationShouldCreateOnlyBookingCancelledEvent() {

        OffsetDateTime now = currentTime();

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Booking booking = persistReservedBooking(userId, showtimeId, now, now.plusMinutes(30));

        BookingResponse response = bookingCancellationService.cancel(userId, booking.getId());

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(response.cancelledAt()).isNotNull();

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        assertThat(reloaded.getCancelledAt()).isNotNull();

        assertThat(reloaded.getCancelledAt()).isEqualTo(response.cancelledAt());

        assertOnlyOutboxEvent(BookingEventContract.BOOKING_CANCELLED, booking.getId());

        assertNoPaymentRequestedEvent();
    }

    @Test
    void expirationShouldCreateOnlyBookingExpiredEvent() {

        OffsetDateTime now = currentTime();

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                persistReservedBooking(
                        userId, showtimeId, now.minusMinutes(20), now.minusMinutes(10));

        boolean expired = bookingExpirationService.expireIfDue(booking.getId());

        entityManager.clear();

        Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();

        assertThat(expired).isTrue();

        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.EXPIRED);

        assertOnlyOutboxEvent(BookingEventContract.BOOKING_EXPIRED, booking.getId());

        assertNoPaymentRequestedEvent();
    }

    private Booking persistPendingBooking(
            UUID userId, UUID showtimeId, OffsetDateTime createdAt, OffsetDateTime expiresAt) {

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "failure-path-" + UuidGenerator.next(),
                        "a".repeat(64),
                        expiresAt,
                        createdAt);

        Booking saved = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(saved.getId(), showtimeId, "H7"));

        return saved;
    }

    private Booking persistReservedBooking(
            UUID userId, UUID showtimeId, OffsetDateTime createdAt, OffsetDateTime expiresAt) {

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "failure-path-" + UuidGenerator.next(),
                        "b".repeat(64),
                        expiresAt,
                        createdAt);

        booking.reserve(TOTAL_AMOUNT, CURRENCY);

        Booking saved = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAndFlush(new BookingSeat(saved.getId(), showtimeId, "H7"));

        return saved;
    }

    private OutboxEventMessage seatReservationRejectedMessage(
            Booking booking, UUID showtimeId, UUID eventId, OffsetDateTime rejectedAt) {

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        booking.getId(),
                        showtimeId,
                        "SEAT_UNAVAILABLE",
                        "One or more requested seats are unavailable",
                        List.of("H7"),
                        rejectedAt);

        return new OutboxEventMessage(
                eventId,
                booking.getId(),
                "BOOKING",
                BookingEventContract.SEAT_RESERVATION_REJECTED,
                BookingEventContract.SEAT_RESERVATION_REJECTED_VERSION,
                rejectedAt,
                BookingEventContract.INVENTORY_PRODUCER,
                UuidGenerator.next(),
                UuidGenerator.next(),
                objectMapper.valueToTree(payload));
    }

    private void assertOnlyOutboxEvent(String expectedEventType, UUID bookingId) {

        List<OutboxEventEntity> events = outboxRepository.findAll();

        assertThat(events).hasSize(1);

        OutboxEventEntity event = events.getFirst();

        assertThat(event.getEventType()).isEqualTo(expectedEventType);

        assertThat(event.getTopic()).isEqualTo(expectedEventType);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(bookingId);

        assertThat(event.getPartitionKey()).isEqualTo(bookingId.toString());

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);
    }

    private void assertNoPaymentRequestedEvent() {

        assertThat(outboxRepository.findAll())
                .noneMatch(
                        event ->
                                BookingEventContract.PAYMENT_REQUESTED.equals(
                                        event.getEventType()));
    }

    private OffsetDateTime currentTime() {

        return OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }
}
