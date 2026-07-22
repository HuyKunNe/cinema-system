package com.cinema.common.kafka.producer;

import com.cinema.common.kafka.event.BaseEvent;

public interface KafkaProducerService {

    void send(
            String topic,
            BaseEvent event);

}
