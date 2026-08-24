package com.cinema.booking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingSeat;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.payload.SeatReservationRejectedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

class SeatReservationRejectedConsumerIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-08-24T10:00:00Z");

    private static final OffsetDateTime EXPIRES_AT = OffsetDateTime.parse("2026-08-24T10:10:00Z");

    @Autowired private SeatReservationRejectedConsumerService consumerService;

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
    void validEventShouldAtomicallyRejectBookingAndStoreMarker() {

        TestContext context = createBooking(false);

        OutboxEventMessage message = rejectedMessage(context);

        SeatReservationRejectedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(result.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.REJECTED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(booking.getRejectionReason()).isEqualTo("SEAT_UNAVAILABLE");

        assertThat(booking.getTotalAmount()).isNull();

        assertThat(booking.getCurrency()).isNull();

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void duplicateEventShouldRejectBookingOnce() {

        TestContext context = createBooking(false);

        OutboxEventMessage message = rejectedMessage(context);

        SeatReservationRejectedConsumerService.Result first =
                consumerService.handle(context.bookingId().toString(), message);

        SeatReservationRejectedConsumerService.Result second =
                consumerService.handle(context.bookingId().toString(), message);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(first.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.REJECTED);

        assertThat(second.status())
                .isEqualTo(SeatReservationRejectedConsumerService.Status.DUPLICATE);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    @Test
    void failedTransitionShouldRollbackProcessedMarker() {

        TestContext context = createBooking(true);

        OutboxEventMessage message = rejectedMessage(context);

        assertThatThrownBy(() -> consumerService.handle(context.bookingId().toString(), message))
                .isInstanceOf(ConflictException.class);

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        assertThat(booking.getRejectionReason()).isEqualTo("INVALID_REQUEST");

        assertThat(processedEventRepository.count()).isZero();
    }

    private TestContext createBooking(boolean alreadyRejected) {

        UUID showtimeId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        UuidGenerator.next(),
                        showtimeId,
                        "rejection-integration-" + UuidGenerator.next(),
                        "a".repeat(64),
                        EXPIRES_AT,
                        NOW);

        if (alreadyRejected) {
            booking.reject("INVALID_REQUEST");
        }

        Booking savedBooking = bookingRepository.saveAndFlush(booking);

        bookingSeatRepository.saveAllAndFlush(
                List.of(new BookingSeat(savedBooking.getId(), showtimeId, "H7")));

        return new TestContext(savedBooking.getId(), showtimeId);
    }

    private OutboxEventMessage rejectedMessage(TestContext context) {

        SeatReservationRejectedPayload payload =
                new SeatReservationRejectedPayload(
                        context.bookingId(),
                        context.showtimeId(),
                        "SEAT_UNAVAILABLE",
                        "One or more requested seats are unavailable",
                        List.of("H7"),
                        NOW);

        return new OutboxEventMessage(
                UuidGenerator.next(),
                context.bookingId(),
                "BOOKING",
                "seat-reservation-rejected",
                "1",
                NOW,
                "inventory-service",
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

    private record TestContext(UUID bookingId, UUID showtimeId) {}
}
