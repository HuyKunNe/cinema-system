package com.cinema.inventory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cinema.inventory.kafka.retry")
public record InventoryKafkaRetryProperties(Duration interval, long maximumRetries) {

    public InventoryKafkaRetryProperties {

        if (interval == null || interval.isNegative() || interval.isZero()) {

            interval = Duration.ofSeconds(1);
        }

        if (maximumRetries < 0) {
            maximumRetries = 3;
        }
    }
}
