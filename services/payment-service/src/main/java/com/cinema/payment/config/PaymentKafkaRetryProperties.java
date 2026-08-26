package com.cinema.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cinema.payment.kafka.retry")
public record PaymentKafkaRetryProperties(Duration interval, long maximumRetries) {

    public PaymentKafkaRetryProperties {

        if (interval == null || interval.isZero() || interval.isNegative()) {
            interval = Duration.ofSeconds(1);
        }

        if (maximumRetries < 0) {
            maximumRetries = 3;
        }
    }
}
