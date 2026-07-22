package com.cinema.common.test.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.cinema.common.test.annotation.IntegrationTest;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractRedisIntegrationTest {

    private static final String REDIS_IMAGE = "redis:7.4-alpine";

    private static final int REDIS_PORT = 6379;

    @Container
    protected static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse(
                    REDIS_IMAGE))
            .withExposedPorts(
                    REDIS_PORT);

    @DynamicPropertySource
    static void registerRedisProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost);

        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(
                        REDIS_PORT));

        registry.add(
                "spring.data.redis.password",
                () -> "");

    }

}
