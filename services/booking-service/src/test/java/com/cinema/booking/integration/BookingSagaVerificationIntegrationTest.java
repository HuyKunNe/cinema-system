package com.cinema.booking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.booking.dto.request.CreateBookingRequest;
import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.enums.BookingStatus;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.event.payload.ReservedSeatPayload;
import com.cinema.booking.event.payload.SeatReservedPayload;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.repository.ProcessedEventRepository;
import com.cinema.booking.service.BookingService;
import com.cinema.booking.service.SeatReservedConsumerService;
import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.NotFoundException;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class BookingSagaVerificationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CLIENT_REQUEST_ID = "booking-saga-verification-1";

    private static final String CURRENCY = "VND";

    private static final BigDecimal FIRST_SEAT_PRICE = new BigDecimal("120000.00");

    private static final BigDecimal SECOND_SEAT_PRICE = new BigDecimal("150000.00");

    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("270000.00");

    @Autowired private BookingService bookingService;

    @Autowired private SeatReservedConsumerService seatReservedConsumerService;

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
    void bookingHappyPathShouldPreserveOwnershipIdempotencyAndCausationChain()
            throws JsonProcessingException {

        UUID userId = UuidGenerator.next();
        UUID otherUserId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        CreateBookingRequest request =
                new CreateBookingRequest(CLIENT_REQUEST_ID, showtimeId, List.of("H8", "H7"));

        BookingResponse created = bookingService.create(userId, request);

        assertPendingBooking(created, userId, showtimeId);

        OutboxEventEntity reservationRequested =
                findOnlyEvent(BookingEventContract.SEAT_RESERVATION_REQUESTED);

        assertSeatReservationRequested(reservationRequested, created, userId, showtimeId);

        UUID seatReservedEventId = UuidGenerator.next();

        OutboxEventMessage seatReservedMessage =
                seatReservedMessage(created, reservationRequested, seatReservedEventId);

        SeatReservedConsumerService.Result firstResult =
                seatReservedConsumerService.handle(created.id().toString(), seatReservedMessage);

        SeatReservedConsumerService.Result duplicateResult =
                seatReservedConsumerService.handle(created.id().toString(), seatReservedMessage);

        entityManager.clear();

        assertThat(firstResult.status()).isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        assertThat(duplicateResult.status())
                .isEqualTo(SeatReservedConsumerService.Status.DUPLICATE);

        BookingResponse reserved = bookingService.findById(userId, created.id());

        assertReservedBooking(reserved, userId, showtimeId);

        assertThatThrownBy(() -> bookingService.findById(otherUserId, created.id()))
                .isInstanceOf(NotFoundException.class);

        assertThat(processedEventRepository.count()).isEqualTo(1);

        assertThat(outboxRepository.count()).isEqualTo(2);

        OutboxEventEntity paymentRequested = findOnlyEvent(BookingEventContract.PAYMENT_REQUESTED);

        assertPaymentRequested(
                paymentRequested, reserved, reservationRequested, seatReservedEventId);
    }

    private void assertPendingBooking(BookingResponse booking, UUID userId, UUID showtimeId) {

        assertThat(booking.id()).isNotNull();

        assertThat(booking.id().version()).isEqualTo(7);

        assertThat(booking.userId()).isEqualTo(userId);

        assertThat(booking.showtimeId()).isEqualTo(showtimeId);

        assertThat(booking.clientRequestId()).isEqualTo(CLIENT_REQUEST_ID);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);

        assertThat(booking.totalAmount()).isNull();

        assertThat(booking.currency()).isNull();

        assertThat(booking.expiresAt()).isNotNull();

        assertThat(booking.seats())
                .extracting(seat -> seat.seatNumber())
                .containsExactly("H7", "H8");

        assertThat(booking.seats())
                .allSatisfy(
                        seat -> {
                            assertThat(seat.inventorySeatId()).isNull();
                            assertThat(seat.seatType()).isNull();
                            assertThat(seat.price()).isNull();
                        });
    }

    private void assertSeatReservationRequested(
            OutboxEventEntity event, BookingResponse booking, UUID userId, UUID showtimeId)
            throws JsonProcessingException {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(booking.id());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);

        assertThat(event.getPartitionKey()).isEqualTo(booking.id().toString());

        assertThat(event.getCorrelationId()).isNotNull();

        assertThat(event.getCorrelationId().version()).isEqualTo(7);

        assertThat(event.getCausationId()).isNull();

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        JsonNode payload = readPayload(event);

        assertThat(payload.path("bookingId").asText()).isEqualTo(booking.id().toString());

        assertThat(payload.path("userId").asText()).isEqualTo(userId.toString());

        assertThat(payload.path("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.path("requestedAt").asText()).isNotBlank();

        assertThat(OffsetDateTime.parse(payload.path("holdExpiresAt").asText()))
                .isEqualTo(booking.expiresAt());

        assertThat(readRequestedSeatNumbers(payload)).containsExactly("H7", "H8");
    }

    private OutboxEventMessage seatReservedMessage(
            BookingResponse booking, OutboxEventEntity reservationRequested, UUID eventId) {

        OffsetDateTime heldAt = booking.expiresAt().minusMinutes(5);

        SeatReservedPayload payload =
                new SeatReservedPayload(
                        booking.id(),
                        booking.showtimeId(),
                        List.of(
                                new ReservedSeatPayload(
                                        UuidGenerator.next(), "H7", "STANDARD", FIRST_SEAT_PRICE),
                                new ReservedSeatPayload(
                                        UuidGenerator.next(), "H8", "PREMIUM", SECOND_SEAT_PRICE)),
                        TOTAL_AMOUNT,
                        CURRENCY,
                        heldAt,
                        booking.expiresAt());

        return new OutboxEventMessage(
                eventId,
                booking.id(),
                "BOOKING",
                BookingEventContract.SEAT_RESERVED,
                BookingEventContract.SEAT_RESERVED_VERSION,
                heldAt,
                BookingEventContract.INVENTORY_PRODUCER,
                reservationRequested.getCorrelationId(),
                reservationRequested.getId(),
                objectMapper.valueToTree(payload));
    }

    private void assertReservedBooking(BookingResponse booking, UUID userId, UUID showtimeId) {

        assertThat(booking.userId()).isEqualTo(userId);

        assertThat(booking.showtimeId()).isEqualTo(showtimeId);

        assertThat(booking.status()).isEqualTo(BookingStatus.RESERVED);

        assertThat(booking.totalAmount()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(booking.currency()).isEqualTo(CURRENCY);

        assertThat(booking.seats())
                .extracting(seat -> seat.seatNumber())
                .containsExactly("H7", "H8");

        assertThat(booking.seats().get(0).inventorySeatId()).isNotNull();

        assertThat(booking.seats().get(0).inventorySeatId().version()).isEqualTo(7);

        assertThat(booking.seats().get(0).seatType()).isEqualTo("STANDARD");

        assertThat(booking.seats().get(0).price()).isEqualByComparingTo(FIRST_SEAT_PRICE);

        assertThat(booking.seats().get(1).inventorySeatId()).isNotNull();

        assertThat(booking.seats().get(1).inventorySeatId().version()).isEqualTo(7);

        assertThat(booking.seats().get(1).seatType()).isEqualTo("PREMIUM");

        assertThat(booking.seats().get(1).price()).isEqualByComparingTo(SECOND_SEAT_PRICE);
    }

    private void assertPaymentRequested(
            OutboxEventEntity event,
            BookingResponse booking,
            OutboxEventEntity reservationRequested,
            UUID seatReservedEventId)
            throws JsonProcessingException {

        assertThat(event.getId()).isNotNull();

        assertThat(event.getId().version()).isEqualTo(7);

        assertThat(event.getAggregateType().name()).isEqualTo("BOOKING");

        assertThat(event.getAggregateId()).isEqualTo(booking.id());

        assertThat(event.getEventType()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(event.getEventVersion())
                .isEqualTo(BookingEventContract.PAYMENT_REQUESTED_VERSION);

        assertThat(event.getTopic()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(event.getPartitionKey()).isEqualTo(booking.id().toString());

        assertThat(event.getCorrelationId()).isEqualTo(reservationRequested.getCorrelationId());

        assertThat(event.getCausationId()).isEqualTo(seatReservedEventId);

        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        JsonNode payload = readPayload(event);

        assertThat(payload.path("bookingId").asText()).isEqualTo(booking.id().toString());

        assertThat(payload.path("userId").asText()).isEqualTo(booking.userId().toString());

        assertThat(payload.path("amount").decimalValue()).isEqualByComparingTo(TOTAL_AMOUNT);

        assertThat(payload.path("currency").asText()).isEqualTo(CURRENCY);

        assertThat(payload.path("paymentAttempt").asInt()).isEqualTo(1);

        assertThat(OffsetDateTime.parse(payload.path("holdExpiresAt").asText()))
                .isEqualTo(booking.expiresAt());

        assertThat(OffsetDateTime.parse(payload.path("requestedAt").asText()))
                .isEqualTo(event.getOccurredAt());
    }

    private OutboxEventEntity findOnlyEvent(String eventType) {

        List<OutboxEventEntity> events =
                outboxRepository.findAll().stream()
                        .filter(event -> eventType.equals(event.getEventType()))
                        .toList();

        assertThat(events).describedAs("Outbox events with type %s", eventType).hasSize(1);

        return events.getFirst();
    }

    private JsonNode readPayload(OutboxEventEntity event) throws JsonProcessingException {

        return objectMapper.readTree(event.getPayload());
    }

    private List<String> readRequestedSeatNumbers(JsonNode payload) {

        List<String> seatNumbers = new ArrayList<>();

        payload.path("seats").forEach(seat -> seatNumbers.add(seat.path("seatNumber").asText()));

        return seatNumbers;
    }

    private void cleanDatabase() {

        outboxRepository.deleteAll();

        processedEventRepository.deleteAll();

        bookingSeatRepository.deleteAll();

        bookingRepository.deleteAll();
    }
}
