package com.cinema.common.kafka.producer;

import static org.mockito.Mockito.verify;

import com.cinema.common.kafka.event.BaseEvent;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.UUID;

class DefaultKafkaEventPublisherTest {

    @Test
    void shouldDelegateExplicitPartitionKey() {
        KafkaProducerService producerService = Mockito.mock(KafkaProducerService.class);

        DefaultKafkaEventPublisher publisher = new DefaultKafkaEventPublisher(producerService);

        TestEvent event = new TestEvent(UUID.randomUUID(), "booking-created", OffsetDateTime.now());

        String topic = "booking-created";

        String partitionKey = UUID.randomUUID().toString();

        publisher.publish(topic, partitionKey, event);

        verify(producerService).send(topic, partitionKey, event);
    }

    private record TestEvent(UUID eventId, String eventType, OffsetDateTime createdAt)
            implements BaseEvent {}
}
