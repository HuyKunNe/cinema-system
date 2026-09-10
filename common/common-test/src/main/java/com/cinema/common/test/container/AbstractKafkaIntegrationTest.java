package com.cinema.common.test.container;

import com.cinema.common.test.annotation.IntegrationTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractKafkaIntegrationTest {

    private static final String KAFKA_IMAGE = "apache/kafka:4.0.0";

    @Container protected static final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE);

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
