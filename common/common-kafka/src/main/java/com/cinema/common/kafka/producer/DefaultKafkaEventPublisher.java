package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

import java.util.Objects;

public class DefaultKafkaEventPublisher implements KafkaEventPublisher {

    private final KafkaProducerService producerService;

    public DefaultKafkaEventPublisher(KafkaProducerService producerService) {

        this.producerService =
                Objects.requireNonNull(producerService, "producerService must not be null");
    }

    @Override
    public void publish(String topic, String partitionKey, BaseEvent event) {

        producerService.send(topic, partitionKey, event);
    }
}
