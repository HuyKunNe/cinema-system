package com.cinema.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.time.Duration;

class BookingKafkaConsumerConfigurationTest {

    private BookingKafkaConsumerConfiguration configuration;

    @BeforeEach
    void setUp() {

        configuration = new BookingKafkaConsumerConfiguration();
    }

    @Test
    void deadLetterRecovererShouldBeCreated() {

        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                configuration.bookingDeadLetterPublishingRecoverer(kafkaTemplate);

        assertThat(recoverer).isNotNull();
    }

    @Test
    void commonErrorHandlerShouldBeDefaultErrorHandler() {

        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                configuration.bookingDeadLetterPublishingRecoverer(kafkaTemplate);

        BookingKafkaRetryProperties properties =
                new BookingKafkaRetryProperties(Duration.ofMillis(10), 2);

        CommonErrorHandler errorHandler =
                configuration.bookingKafkaErrorHandler(recoverer, properties);

        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }
}
