package com.cinema.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.Duration;

class BookingKafkaRetryPropertiesTest {

    @Test
    void validValuesShouldBePreserved() {

        BookingKafkaRetryProperties properties =
                new BookingKafkaRetryProperties(Duration.ofMillis(500), 5);

        assertThat(properties.interval()).isEqualTo(Duration.ofMillis(500));

        assertThat(properties.maximumRetries()).isEqualTo(5);
    }

    @Test
    void nullIntervalShouldUseDefault() {

        BookingKafkaRetryProperties properties = new BookingKafkaRetryProperties(null, 3);

        assertThat(properties.interval()).isEqualTo(Duration.ofSeconds(1));

        assertThat(properties.maximumRetries()).isEqualTo(3);
    }

    @Test
    void zeroIntervalShouldUseDefault() {

        BookingKafkaRetryProperties properties = new BookingKafkaRetryProperties(Duration.ZERO, 3);

        assertThat(properties.interval()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void negativeIntervalShouldUseDefault() {

        BookingKafkaRetryProperties properties =
                new BookingKafkaRetryProperties(Duration.ofSeconds(-1), 3);

        assertThat(properties.interval()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    void negativeMaximumRetriesShouldUseDefault() {

        BookingKafkaRetryProperties properties =
                new BookingKafkaRetryProperties(Duration.ofMillis(100), -1);

        assertThat(properties.interval()).isEqualTo(Duration.ofMillis(100));

        assertThat(properties.maximumRetries()).isEqualTo(3);
    }
}
