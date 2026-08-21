package com.cinema.booking.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.booking.dto.response.BookingResponse;
import com.cinema.booking.event.BookingEventContract;
import com.cinema.booking.repository.BookingRepository;
import com.cinema.booking.repository.BookingSeatRepository;
import com.cinema.booking.service.BookingCreationService;
import com.cinema.booking.testsupport.MutableTestClock;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Import(SeatReservationRequestedPublicationIntegrationTest.ClockConfiguration.class)
class SeatReservationRequestedPublicationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-08-21T10:00:00Z");

    @Autowired private BookingCreationService bookingCreationService;

    @Autowired private BookingRepository bookingRepository;

    @Autowired private BookingSeatRepository bookingSeatRepository;

    @Autowired private OutboxRepository outboxRepository;

    @Autowired private OutboxClaimService claimService;

    @Autowired private OutboxPublisher publisher;

    @Autowired private OutboxAcknowledgementService acknowledgementService;

    @Autowired private MutableTestClock clock;

    @MockitoBean private KafkaProducerService kafkaProducerService;

    private OutboxScheduler scheduler;

    @BeforeEach
    void setUp() {

        clock.set(INITIAL_TIME);

        scheduler = new OutboxScheduler(claimService, publisher, acknowledgementService);

        outboxRepository.deleteAll();
        bookingSeatRepository.deleteAll();
        bookingRepository.deleteAll();
    }

    @Test
    void shouldPublishCanonicalEnvelopeAndMarkOutboxSent() {

        when(kafkaProducerService.send(anyString(), anyString(), any(BaseEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        BookingResponse booking =
                bookingCreationService.createNew(
                        userId, showtimeId, "request-1", "a".repeat(64), List.of("H7", "H8"));

        OutboxEventEntity pending = outboxRepository.findAll().getFirst();

        UUID originalEventId = pending.getId();

        UUID originalCorrelationId = pending.getCorrelationId();

        assertThat(pending.getStatus()).isEqualTo(OutboxStatus.PENDING);

        scheduler.publishPendingEvents();

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);

        verify(kafkaProducerService)
                .send(topicCaptor.capture(), keyCaptor.capture(), eventCaptor.capture());

        assertThat(topicCaptor.getValue())
                .isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);

        assertThat(keyCaptor.getValue()).isEqualTo(booking.id().toString());

        assertThat(eventCaptor.getValue()).isInstanceOf(OutboxEventMessage.class);

        OutboxEventMessage message = (OutboxEventMessage) eventCaptor.getValue();

        assertThat(message.eventId()).isEqualTo(originalEventId);

        assertThat(message.eventId().version()).isEqualTo(7);

        assertThat(message.aggregateId()).isEqualTo(booking.id());

        assertThat(message.aggregateType()).isEqualTo("BOOKING");

        assertThat(message.eventType()).isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED);

        assertThat(message.eventVersion())
                .isEqualTo(BookingEventContract.SEAT_RESERVATION_REQUESTED_VERSION);

        assertThat(message.producer()).isEqualTo("booking-service");

        assertThat(message.correlationId()).isEqualTo(originalCorrelationId);

        assertThat(message.correlationId().version()).isEqualTo(7);

        assertThat(message.causationId()).isNull();

        assertThat(message.occurredAt())
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertPayload(message.payload(), booking.id(), userId, showtimeId);

        OutboxEventEntity sent = outboxRepository.findById(originalEventId).orElseThrow();

        assertThat(sent.getStatus()).isEqualTo(OutboxStatus.SENT);

        assertThat(sent.getPublishedAt())
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertThat(sent.getNextAttemptAt()).isNull();
        assertThat(sent.getProcessingOwner()).isNull();
        assertThat(sent.getProcessingStartedAt()).isNull();
        assertThat(sent.getProcessingExpiresAt()).isNull();
        assertThat(sent.getRetryCount()).isZero();
    }

    @Test
    void failedPublicationShouldRetrySameEventIdAndPartitionKey() {

        RuntimeException kafkaFailure = new RuntimeException("Kafka broker unavailable");

        when(kafkaProducerService.send(anyString(), anyString(), any(BaseEvent.class)))
                .thenReturn(CompletableFuture.failedFuture(kafkaFailure))
                .thenReturn(CompletableFuture.completedFuture(null));

        UUID userId = UuidGenerator.next();
        UUID showtimeId = UuidGenerator.next();

        BookingResponse booking =
                bookingCreationService.createNew(
                        userId, showtimeId, "request-retry-1", "b".repeat(64), List.of("H7"));

        OutboxEventEntity original = outboxRepository.findAll().getFirst();

        UUID originalEventId = original.getId();

        String originalPartitionKey = original.getPartitionKey();

        UUID originalCorrelationId = original.getCorrelationId();

        scheduler.publishPendingEvents();

        OutboxEventEntity failed = outboxRepository.findById(originalEventId).orElseThrow();

        assertThat(failed.getStatus()).isEqualTo(OutboxStatus.FAILED);

        assertThat(failed.getRetryCount()).isEqualTo(1);

        assertThat(failed.getLastError()).isEqualTo("Kafka broker unavailable");

        assertThat(failed.getNextAttemptAt())
                .isAfter(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertThat(failed.getProcessingOwner()).isNull();

        clock.advance(Duration.ofSeconds(2));

        scheduler.publishPendingEvents();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<BaseEvent> eventCaptor = ArgumentCaptor.forClass(BaseEvent.class);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

        verify(kafkaProducerService, times(2))
                .send(
                        org.mockito.ArgumentMatchers.eq(
                                BookingEventContract.SEAT_RESERVATION_REQUESTED),
                        keyCaptor.capture(),
                        eventCaptor.capture());

        assertThat(keyCaptor.getAllValues())
                .containsExactly(originalPartitionKey, originalPartitionKey);

        List<OutboxEventMessage> messages =
                eventCaptor.getAllValues().stream().map(OutboxEventMessage.class::cast).toList();

        assertThat(messages)
                .extracting(OutboxEventMessage::eventId)
                .containsExactly(originalEventId, originalEventId);

        assertThat(messages)
                .extracting(OutboxEventMessage::aggregateId)
                .containsExactly(booking.id(), booking.id());

        assertThat(messages)
                .extracting(OutboxEventMessage::correlationId)
                .containsExactly(originalCorrelationId, originalCorrelationId);

        OutboxEventEntity sent = outboxRepository.findById(originalEventId).orElseThrow();

        assertThat(sent.getStatus()).isEqualTo(OutboxStatus.SENT);

        assertThat(sent.getRetryCount()).isEqualTo(1);

        assertThat(sent.getPublishedAt())
                .isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME.plusSeconds(2), ZoneOffset.UTC));

        assertThat(sent.getNextAttemptAt()).isNull();
        assertThat(sent.getProcessingOwner()).isNull();
        assertThat(sent.getLastError()).isNull();
    }

    private void assertPayload(JsonNode payload, UUID bookingId, UUID userId, UUID showtimeId) {

        assertThat(payload.get("bookingId").asText()).isEqualTo(bookingId.toString());

        assertThat(payload.get("userId").asText()).isEqualTo(userId.toString());

        assertThat(payload.get("showtimeId").asText()).isEqualTo(showtimeId.toString());

        assertThat(payload.get("seats"))
                .extracting(seat -> seat.get("seatNumber").asText())
                .containsExactly("H7", "H8");

        OffsetDateTime requestedAt = OffsetDateTime.parse(payload.get("requestedAt").asText());

        OffsetDateTime holdExpiresAt = OffsetDateTime.parse(payload.get("holdExpiresAt").asText());

        assertThat(requestedAt).isEqualTo(OffsetDateTime.ofInstant(INITIAL_TIME, ZoneOffset.UTC));

        assertThat(holdExpiresAt).isEqualTo(requestedAt.plusMinutes(10));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfiguration {

        @Bean
        @Primary
        MutableTestClock mutableTestClock() {

            return new MutableTestClock(INITIAL_TIME, ZoneOffset.UTC);
        }
    }
}
