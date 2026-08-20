package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;
import com.cinema.common.kafka.serializer.KafkaEventSerializer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DefaultKafkaProducerService implements KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaEventSerializer serializer;

    public DefaultKafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate, KafkaEventSerializer serializer) {

        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;
    }

    @Override
    public CompletableFuture<Void> send(String topic, String partitionKey, BaseEvent event) {

        String serializedEvent = serializer.serialize(event);

        return kafkaTemplate.send(topic, partitionKey, serializedEvent).thenApply(result -> null);
    }
}
