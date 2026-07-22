package com.cinema.common.kafka.exception;

public class KafkaPublishException
        extends RuntimeException {

    public KafkaPublishException(
            Throwable cause) {

        super(
                "Kafka publish failed",
                cause);

    }

}
