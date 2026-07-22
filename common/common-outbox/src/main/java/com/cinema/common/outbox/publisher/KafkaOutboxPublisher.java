package com.cinema.common.outbox.publisher;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import com.cinema.common.kafka.producer.KafkaProducerService;
import com.cinema.common.outbox.entity.OutboxEventEntity;
import com.cinema.common.outbox.exception.OutboxPublishException;
import com.cinema.common.outbox.model.OutboxEventMessage;
import com.cinema.common.outbox.model.OutboxEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KafkaOutboxPublisher
        implements OutboxPublisher {

    private final KafkaProducerService producer;

    private final ObjectMapper mapper;

    public KafkaOutboxPublisher(
            KafkaProducerService producer,
            ObjectMapper mapper) {

        this.producer = producer;

        this.mapper = mapper;

    }

    @Override
    public CompletableFuture<Void> publish(
            OutboxEventEntity event) {

        try {

            OutboxEventPayload payload = mapper.readValue(
                    event.getPayload(),
                    OutboxEventPayload.class);

            OutboxEventMessage message = new OutboxEventMessage(
                    event.getId(),
                    event.getEventType(),
                    event.getCreatedAt(),
                    payload);

            return producer.send(
                    event.getEventType(),
                    message);

        } catch (Exception exception) {

            CompletableFuture<Void> future = new CompletableFuture<>();

            future.completeExceptionally(
                    new OutboxPublishException(exception));

            return future;

        }

    }

}
