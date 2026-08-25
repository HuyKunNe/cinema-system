package com.cinema.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.cinema.common.kafka.event.BaseEvent;
import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.acknowledgement.OutboxAcknowledgementService;
import com.cinema.common.outbox.claim.OutboxClaimService;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.OutboxStatus;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.publisher.OutboxPublisher;
import com.cinema.common.outbox.repository.OutboxRepository;
import com.cinema.common.outbox.scheduler.OutboxScheduler;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class PaymentRequestedKafkaPublicationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-25T10:00:00Z");

    private static final OffsetDateTime HELD_AT =
            OffsetDateTime.ofInstant(INITIAL_TIME.minusSeconds(30), ZoneOffset.UTC);

    private static final OffsetDateTime HOLD_EXPIRES_AT =
            OffsetDateTime.ofInstant(INITIAL_TIME.plusSeconds(570), ZoneOffset.UTC);

    @Autowired private SeatReservedConsumerService consumerService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private ProcessedEventRepository processedEventRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private OutboxClaimService claimService;

    @Autowired private OutboxPublisher publisher;

    @Autowired private OutboxAcknowledgementService acknowledgementService;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private EntityManager entityManager;

    @MockitoBean private KafkaProducerService kafkaProducerService;

    @MockitoBean(name = "systemClock")
    private Clock systemClock;

    private OutboxScheduler scheduler;

    @BeforeEach
    void setUp() {

        when(systemClock.instant()).thenReturn(INITIAL_TIME);

        when(systemClock.getZone()).thenReturn(ZoneOffset.UTC);

        scheduler = new OutboxScheduler(claimService, publisher, acknowledgementService);

        outboxRepository.deleteAll();
        processedEventRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void shouldPublishCanonicalPaymentRequestedEnvelopeAndMarkSent() {

        when(kafkaProducerService.send(anyString(), anyString(), any(BaseEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        TestContext context = createPendingBooking();

        OutboxEventMessage sourceEvent = seatReservedMessage(context);

        SeatReservedConsumerService.Result result =
                consumerService.handle(context.bookingId().toString(), sourceEvent);

        assertThat(result.status()).isEqualTo(SeatReservedConsumerService.Status.RESERVED);

        List<OutboxEventEntity> pendingEvents = paymentRequestedEvents();

        assertThat(pendingEvents).hasSize(1);

        OutboxEventEntity pending = pendingEvents.getFirst();

        UUID originalPaymentEventId = pending.getId();

        assertThat(pending.getStatus()).isEqualTo(OutboxStatus.PENDING);

        assertThat(pending.getCorrelationId()).isEqualTo(sourceEvent.correlationId());

        assertThat(pending.getCausationId()).isEqualTo(sourceEvent.eventId());

        scheduler.publishPendingEvents();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);

        verify(kafkaProducerService)
                .send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(keyCaptor.getValue()).isEqualTo(context.bookingId().toString());

        assertThat(eventCaptor.getValue()).isInstanceOf(OutboxEventMessage.class);

        OutboxEventMessage publishedMessage = (OutboxEventMessage) eventCaptor.getValue();

        assertThat(publishedMessage.eventId()).isEqualTo(originalPaymentEventId);

        assertThat(publishedMessage.eventId().version()).isEqualTo(7);

        assertThat(publishedMessage.aggregateId()).isEqualTo(context.bookingId());

        assertThat(publishedMessage.aggregateType()).isEqualTo("BOOKING");

        assertThat(publishedMessage.eventType()).isEqualTo(BookingEventContract.PAYMENT_REQUESTED);

        assertThat(publishedMessage.eventVersion())
                .isEqualTo(BookingEventContract.PAYMENT_REQUESTED_VERSION);

        assertThat(publishedMessage.producer()).isEqualTo("booking-service");

        assertThat(publishedMessage.correlationId()).isEqualTo(sourceEvent.correlationId());

        assertThat(publishedMessage.causationId()).isEqualTo(sourceEvent.eventId());

        assertThat(publishedMessage.occurredAt())
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertPayload(publishedMessage.payload(), context);

        OutboxEventEntity sent = outboxRepository.findById(originalPaymentEventId).orElseThrow();

        assertThat(sent.getStatus()).isEqualTo(OutboxStatus.SENT);

        assertThat(sent.getPublishedAt())
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertThat(sent.getNextAttemptAt()).isNull();

        assertThat(sent.getProcessingOwner()).isNull();

        assertThat(sent.getProcessingStartedAt()).isNull();

        assertThat(sent.getProcessingExpiresAt()).isNull();

        assertThat(sent.getRetryCount()).isZero();

        entityManager.clear();

        Booking booking = bookingRepository.findById(context.bookingId()).orElseThrow();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.RESERVED);

        assertThat(processedEventRepository.count()).isEqualTo(1);
    }

    private void assertPayload(JsonNode payload, TestContext context) {

        assertThat(payload.size()).isEqualTo(7);

        assertThat(payload.get("bookingId").asText()).isEqualTo(context.bookingId().toString());

        assertThat(payload.get("userId").asText()).isEqualTo(context.userId().toString());

        assertThat(payload.get("amount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("180000.00"));

        assertThat(payload.get("currency").asText()).isEqualTo("VND");

        assertThat(payload.get("paymentAttempt").asInt()).isEqualTo(1);

        assertThat(OffsetDateTime.parse(payload.get("holdExpiresAt").asText()))
                .isEqualTo(HOLD_EXPIRES_AT);

        assertThat(OffsetDateTime.parse(payload.get("requestedAt").asText()))
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertThat(payload.has("showtimeId")).isFalse();

        assertThat(payload.has("seats")).isFalse();

        assertThat(payload.has("provider")).isFalse();

        assertThat(payload.has("providerReference")).isFalse();

        assertThat(payload.has("cardNumber")).isFalse();

        assertThat(payload.has("cvv")).isFalse();
    }

    private TestContext createPendingBooking() {

        UUID userId = UuidGenerator.next();

        UUID showtimeId = UuidGenerator.next();

        UUID inventorySeatId = UuidGenerator.next();

        Booking booking =
                new Booking(
                        userId,
                        showtimeId,
                        "payment-kafka-" + UuidGenerator.next(),
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

    private record TestContext(
            UUID bookingId, UUID userId, UUID showtimeId, UUID inventorySeatId) {}
}
