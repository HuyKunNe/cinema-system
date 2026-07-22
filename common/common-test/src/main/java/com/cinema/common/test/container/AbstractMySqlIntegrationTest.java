package com.cinema.common.test.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.cinema.common.test.annotation.IntegrationTest;

@IntegrationTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:8.4.0";

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>(MYSQL_IMAGE)
            .withDatabaseName(
                    "cinema_test")
            .withUsername(
                    "cinema")
            .withPassword(
                    "cinema");

    @DynamicPropertySource
    static void registerMySqlProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                MYSQL::getJdbcUrl);

        registry.add(
                "spring.datasource.username",
                MYSQL::getUsername);

        registry.add(
                "spring.datasource.password",
                MYSQL::getPassword);

        registry.add(
                "spring.datasource.driver-class-name",
                MYSQL::getDriverClassName);

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate");

        registry.add(
                "spring.flyway.enabled",
                () -> true);

    }

}
