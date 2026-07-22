package com.cinema.common.kafka.consumer;

import org.springframework.stereotype.Component;

@Component
public class KafkaErrorHandler {

    public void handle(
            Exception exception) {

        // logging handled by common-logging

    }

}
