package com.cinema.common.kafka.producer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.common.kafka.event.BaseEvent;
import com.cinema.common.kafka.serializer.KafkaEventSerializer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class DefaultKafkaProducerServiceTest {

    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaEventSerializer serializer;

    private DefaultKafkaProducerService producerService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);

        serializer = mock(KafkaEventSerializer.class);

        producerService = new DefaultKafkaProducerService(kafkaTemplate, serializer);
    }

    @Test
    void shouldSendUsingExplicitPartitionKey() {
        UUID eventId = UUID.randomUUID();

        TestEvent event = new TestEvent(eventId, "booking-created", OffsetDateTime.now());

        String topic = "booking-created";

        String bookingId = UUID.randomUUID().toString();

        String serializedEvent = "{\"eventId\":\"" + eventId + "\"}";

        SendResult<String, String> sendResult = mock(SendResult.class);

        when(serializer.serialize(event)).thenReturn(serializedEvent);

        when(kafkaTemplate.send(topic, bookingId, serializedEvent))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producerService.send(topic, bookingId, event).join();

        verify(serializer).serialize(event);

        verify(kafkaTemplate).send(topic, bookingId, serializedEvent);
    }

    @Test
    void legacyOverloadShouldUseEventIdAsDefaultKey() {
        UUID eventId = UUID.randomUUID();

        TestEvent event = new TestEvent(eventId, "test-event", OffsetDateTime.now());

        String serializedEvent = "{\"eventId\":\"" + eventId + "\"}";

        SendResult<String, String> sendResult = mock(SendResult.class);

        when(serializer.serialize(event)).thenReturn(serializedEvent);

        when(kafkaTemplate.send("test-event", eventId.toString(), serializedEvent))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producerService.send("test-event", event).join();

        verify(kafkaTemplate).send("test-event", eventId.toString(), serializedEvent);
    }

    private record TestEvent(UUID eventId, String eventType, OffsetDateTime createdAt)
            implements BaseEvent {}
}
