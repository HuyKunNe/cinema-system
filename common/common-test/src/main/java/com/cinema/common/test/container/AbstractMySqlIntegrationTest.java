package com.cinema.common.test.container;

import com.cinema.common.test.annotation.IntegrationTest;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@IntegrationTest
public abstract class AbstractMySqlIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:8.4.0";

    protected static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>(MYSQL_IMAGE)
                    .withDatabaseName("cinema_test_db")
                    .withUsername("cinema")
                    .withPassword("cinema");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerMySqlProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);

        registry.add("spring.datasource.username", MYSQL::getUsername);

        registry.add("spring.datasource.password", MYSQL::getPassword);

        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.flyway.enabled", () -> true);
    }
}
