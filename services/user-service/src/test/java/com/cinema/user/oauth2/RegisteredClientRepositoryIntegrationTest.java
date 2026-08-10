package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

class RegisteredClientRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void shouldUseJdbcRegisteredClientRepository() {
        assertThat(registeredClientRepository)
                .isInstanceOf(
                        JdbcRegisteredClientRepository.class);
    }
}
