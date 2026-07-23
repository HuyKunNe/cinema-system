package com.cinema.movie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(classes = MovieServiceApplication.class, properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false"
})
class MovieServiceApplicationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            "mysql:8.4")
            .withDatabaseName(
                    "movie_test_db")
            .withUsername(
                    "test")
            .withPassword(
                    "test");

    @DynamicPropertySource
    static void registerProperties(
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
                "spring.jpa.hibernate.ddl-auto",
                () -> "validate");

        registry.add(
                "spring.flyway.enabled",
                () -> true);
    }

    @Test
    void contextLoads() {
    }
}
