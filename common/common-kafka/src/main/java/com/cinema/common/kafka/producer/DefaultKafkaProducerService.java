package com.cinema.common.kafka.producer;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.cinema.common.kafka.event.BaseEvent;
import com.cinema.common.kafka.serializer.KafkaEventSerializer;

@Service
public class DefaultKafkaProducerService
        implements KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final KafkaEventSerializer serializer;

    public DefaultKafkaProducerService(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaEventSerializer serializer) {

        this.kafkaTemplate = kafkaTemplate;
        this.serializer = serializer;

    }

    @Override
    public CompletableFuture<Void> send(
            String topic,
            BaseEvent event) {

        return kafkaTemplate.send(
                topic,
                event.eventId().toString(),
                serializer.serialize(event)

        ).thenApply(
                result -> null);

    }

}
