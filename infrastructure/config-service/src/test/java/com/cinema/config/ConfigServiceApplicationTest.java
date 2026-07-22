package com.cinema.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import com.cinema.common.test.annotation.IntegrationTest;

@ActiveProfiles({
        "test",
        "native"
})
@IntegrationTest(classes = ConfigServiceApplication.class)
class ConfigServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Test
    void shouldLoadApplicationContext() {

        assertNotNull(
                applicationContext);

    }

    @Test
    void shouldCreateEnvironmentRepository() {

        assertNotNull(
                environmentRepository);

    }

}
