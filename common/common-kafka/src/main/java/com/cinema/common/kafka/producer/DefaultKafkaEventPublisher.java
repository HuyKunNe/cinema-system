package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

import org.springframework.stereotype.Component;

@Component
public class DefaultKafkaEventPublisher implements KafkaEventPublisher {

    private final KafkaProducerService producerService;

    public DefaultKafkaEventPublisher(KafkaProducerService producerService) {

        this.producerService = producerService;
    }

    @Override
    public void publish(String topic, String partitionKey, BaseEvent event) {

        producerService.send(topic, partitionKey, event);
    }
}
