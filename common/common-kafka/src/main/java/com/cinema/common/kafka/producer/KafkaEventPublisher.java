package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

public interface KafkaEventPublisher {

    void publish(
            String topic,
            BaseEvent event);

}
