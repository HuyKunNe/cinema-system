package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

public interface KafkaEventPublisher {

    void publish(String topic, String partitionKey, BaseEvent event);

    default void publish(String topic, BaseEvent event) {

        publish(topic, event.eventId().toString(), event);
    }
}
