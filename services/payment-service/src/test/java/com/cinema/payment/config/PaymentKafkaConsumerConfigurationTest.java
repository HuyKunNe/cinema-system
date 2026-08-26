package com.cinema.payment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

import java.time.Duration;

class PaymentKafkaConsumerConfigurationTest {

    private PaymentKafkaConsumerConfiguration configuration;

    @BeforeEach
    void setUp() {

        configuration = new PaymentKafkaConsumerConfiguration();
    }

    @Test
    void deadLetterRecovererShouldBeCreated() {

        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                configuration.paymentDeadLetterPublishingRecoverer(kafkaTemplate);

        assertThat(recoverer).isNotNull();
    }

    @Test
    void commonErrorHandlerShouldBeDefaultErrorHandler() {

        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);

        DeadLetterPublishingRecoverer recoverer =
                configuration.paymentDeadLetterPublishingRecoverer(kafkaTemplate);

        PaymentKafkaRetryProperties properties =
                new PaymentKafkaRetryProperties(Duration.ofMillis(10), 2);

        CommonErrorHandler errorHandler =
                configuration.paymentKafkaErrorHandler(recoverer, properties);

        assertThat(errorHandler).isInstanceOf(DefaultErrorHandler.class);
    }
}
