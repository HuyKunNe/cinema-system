package com.cinema.payment.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class PaymentKafkaRetryPropertiesTest {

    @Test
    void validValuesShouldBePreserved() {

        PaymentKafkaRetryProperties properties =
                new PaymentKafkaRetryProperties(Duration.ofMillis(250), 5);

        assertThat(properties.interval()).isEqualTo(Duration.ofMillis(250));

        assertThat(properties.maximumRetries()).isEqualTo(5);
    }

    @Test
    void missingAndNegativeValuesShouldUseDefaults() {

        PaymentKafkaRetryProperties properties = new PaymentKafkaRetryProperties(null, -1);

        assertThat(properties.interval()).isEqualTo(Duration.ofSeconds(1));

        assertThat(properties.maximumRetries()).isEqualTo(3);
    }

    @Test
    void zeroRetryShouldBeAllowed() {

        PaymentKafkaRetryProperties properties =
                new PaymentKafkaRetryProperties(Duration.ofMillis(10), 0);

        assertThat(properties.maximumRetries()).isZero();
    }

    @Test
    void nonPositiveIntervalShouldUseDefault() {

        PaymentKafkaRetryProperties zero = new PaymentKafkaRetryProperties(Duration.ZERO, 1);

        PaymentKafkaRetryProperties negative =
                new PaymentKafkaRetryProperties(Duration.ofMillis(-1), 1);

        assertThat(zero.interval()).isEqualTo(Duration.ofSeconds(1));

        assertThat(negative.interval()).isEqualTo(Duration.ofSeconds(1));
    }
}
