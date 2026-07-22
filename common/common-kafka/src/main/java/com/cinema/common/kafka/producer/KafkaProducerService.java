package com.cinema.common.kafka.producer;

import java.util.concurrent.CompletableFuture;

import com.cinema.common.kafka.event.BaseEvent;

public interface KafkaProducerService {

    CompletableFuture<Void> send(
            String topic,
            BaseEvent event);

}
