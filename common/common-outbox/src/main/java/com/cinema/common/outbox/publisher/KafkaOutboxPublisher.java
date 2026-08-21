package com.cinema.common.outbox.publisher;

import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.config.OutboxProperties;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.exception.OutboxPublishException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.CompletableFuture;

public class KafkaOutboxPublisher implements OutboxPublisher {

    private final KafkaProducerService producer;

    private final ObjectMapper mapper;

    private final OutboxProperties properties;

    public KafkaOutboxPublisher(
            KafkaProducerService producer, ObjectMapper mapper, OutboxProperties properties) {

        this.producer = producer;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public CompletableFuture<Void> publish(OutboxEventEntity event) {

        try {
            JsonNode payload = mapper.readTree(event.getPayload());

            OutboxEventMessage message =
                    new OutboxEventMessage(
                            event.getId(),
                            event.getAggregateId(),
                            event.getAggregateType().name(),
                            event.getEventType(),
                            event.getEventVersion(),
                            event.getOccurredAt(),
                            properties.producer(),
                            event.getCorrelationId(),
                            event.getCausationId(),
                            payload);

            return producer.send(event.getTopic(), event.getPartitionKey(), message);

        } catch (Exception exception) {
            return CompletableFuture.failedFuture(new OutboxPublishException(exception));
        }
    }
}
