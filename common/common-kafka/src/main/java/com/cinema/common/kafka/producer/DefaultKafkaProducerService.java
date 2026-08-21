package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;
import com.cinema.common.kafka.serializer.KafkaEventSerializer;

import org.springframework.kafka.core.KafkaTemplate;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DefaultKafkaProducerService implements KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaEventSerializer serializer;

    public DefaultKafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate, KafkaEventSerializer serializer) {

        this.kafkaTemplate =
                Objects.requireNonNull(kafkaTemplate, "kafkaTemplate must not be null");

        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
    }

    @Override
    public CompletableFuture<Void> send(String topic, String partitionKey, BaseEvent event) {

        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(partitionKey, "partitionKey must not be null");
        Objects.requireNonNull(event, "event must not be null");

        String serializedEvent = serializer.serialize(event);

        return kafkaTemplate.send(topic, partitionKey, serializedEvent).thenApply(result -> null);
    }
}
