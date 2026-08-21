package com.cinema.common.outbox.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.config.OutboxProperties;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.enums.AggregateType;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class KafkaOutboxPublisherTest {

    private final KafkaProducerService producer = mock(KafkaProducerService.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final OutboxProperties properties =
            new OutboxProperties(
                    "booking-service",
                    100,
                    Duration.ofSeconds(5),
                    Duration.ofSeconds(30),
                    5,
                    Duration.ofSeconds(1),
                    Duration.ofMinutes(1),
                    Duration.ZERO);

    private final KafkaOutboxPublisher publisher =
            new KafkaOutboxPublisher(producer, objectMapper, properties);

    @Test
    void shouldPublishCanonicalEnvelopeUsingPersistedTopicAndPartitionKey() {

        UUID eventId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        UUID causationId = UUID.randomUUID();

        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-21T10:00:00Z");
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-21T10:00:01Z");

        OutboxEventEntity event =
                new OutboxEventEntity(
                        eventId,
                        AggregateType.BOOKING,
                        bookingId,
                        "seat-reservation-requested",
                        "1",
                        "seat-reservation-requested",
                        bookingId.toString(),
                        occurredAt,
                        correlationId,
                        causationId,
                        """
                        {
                          "bookingId": "%s",
                          "seatNumbers": ["H7", "H8"]
                        }
                        """
                                .formatted(bookingId),
                        createdAt);

        ArgumentCaptor<OutboxEventMessage> messageCaptor =
                ArgumentCaptor.forClass(OutboxEventMessage.class);

        when(producer.send(
                        eq("seat-reservation-requested"),
                        eq(bookingId.toString()),
                        org.mockito.ArgumentMatchers.any(OutboxEventMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event).join();

        verify(producer)
                .send(
                        eq("seat-reservation-requested"),
                        eq(bookingId.toString()),
                        messageCaptor.capture());

        OutboxEventMessage message = messageCaptor.getValue();

        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.aggregateId()).isEqualTo(bookingId);
        assertThat(message.aggregateType()).isEqualTo("BOOKING");
        assertThat(message.eventType()).isEqualTo("seat-reservation-requested");
        assertThat(message.eventVersion()).isEqualTo("1");
        assertThat(message.occurredAt()).isEqualTo(occurredAt);
        assertThat(message.producer()).isEqualTo("booking-service");
        assertThat(message.correlationId()).isEqualTo(correlationId);
        assertThat(message.causationId()).isEqualTo(causationId);
        assertThat(message.payload().get("bookingId").asText()).isEqualTo(bookingId.toString());
        assertThat(message.payload().get("seatNumbers")).hasSize(2);
    }

    @Test
    void malformedPayloadShouldCompleteFutureExceptionally() {

        OutboxEventEntity event =
                new OutboxEventEntity(
                        UUID.randomUUID(),
                        AggregateType.BOOKING,
                        UUID.randomUUID(),
                        "seat-reservation-requested",
                        "1",
                        "seat-reservation-requested",
                        UUID.randomUUID().toString(),
                        OffsetDateTime.parse("2026-08-21T10:00:00Z"),
                        UUID.randomUUID(),
                        null,
                        "{invalid-json",
                        OffsetDateTime.parse("2026-08-21T10:00:01Z"));

        CompletableFuture<Void> result = publisher.publish(event);

        assertThat(result).isCompletedExceptionally();
    }
}
