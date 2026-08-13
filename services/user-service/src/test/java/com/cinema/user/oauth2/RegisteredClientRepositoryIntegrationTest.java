package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.user.oauth2.impl.ActiveRegisteredClientRepository;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

class RegisteredClientRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldUseJdbcRegisteredClientRepository() {
        assertThat(registeredClientRepository)
                .isInstanceOf(
                        JdbcRegisteredClientRepository.class);
    }

    @Test
    void shouldUseActiveRegisteredClientRepository() {
        assertThat(registeredClientRepository)
                .isInstanceOf(
                        ActiveRegisteredClientRepository.class);
    }

    @Test
    void inactiveClientShouldNotBeAvailableByEitherLookup() {
        RegisteredClient client = RegisteredClient.withId(
                UUID.randomUUID().toString())
                .clientId(
                        "inactive-client-"
                                + UUID.randomUUID())
                .clientName(
                        "Inactive Client")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "http://127.0.0.1:3000/callback")
                .scope(
                        "openid")
                .build();

        registeredClientRepository.save(
                client);

        jdbcTemplate.update(
                """
                        UPDATE oauth2_registered_client
                        SET active = FALSE
                        WHERE id = ?
                        """,
                client.getId());

        assertThat(registeredClientRepository.findById(
                client.getId()))
                .isNull();

        assertThat(registeredClientRepository.findByClientId(
                client.getClientId()))
                .isNull();
    }
}
