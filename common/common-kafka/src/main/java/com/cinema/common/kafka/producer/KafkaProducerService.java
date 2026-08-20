package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

import java.util.concurrent.CompletableFuture;

public interface KafkaProducerService {

    CompletableFuture<Void> send(String topic, String partitionKey, BaseEvent event);

    default CompletableFuture<Void> send(String topic, BaseEvent event) {

        return send(topic, event.eventId().toString(), event);
    }
}
