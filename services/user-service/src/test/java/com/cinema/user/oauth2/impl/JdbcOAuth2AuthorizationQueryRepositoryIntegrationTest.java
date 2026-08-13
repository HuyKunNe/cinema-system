package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.oauth2.OAuth2AuthorizationQueryRepository;

@Transactional
class JdbcOAuth2AuthorizationQueryRepositoryIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String CLIENT_ID = "r25-11-8-query-client";

    private static final String RAW_CLIENT_SECRET = "r25-11-8-query-client-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String PRINCIPAL_NAME = "revocation-query-user@example.com";

    private static final String OTHER_PRINCIPAL_NAME = "other-revocation-user@example.com";

    private static final String SCOPE = "booking:read";

    private static final Instant ISSUED_AT = Instant.parse(
            "2026-08-13T07:00:00Z");

    @Autowired
    private OAuth2AuthorizationQueryRepository queryRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldFindAuthorizationIdsByPrincipalName() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2Authorization first = saveAuthorization(
                client,
                PRINCIPAL_NAME);

        OAuth2Authorization second = saveAuthorization(
                client,
                PRINCIPAL_NAME);

        saveAuthorization(
                client,
                OTHER_PRINCIPAL_NAME);

        assertThat(queryRepository
                .findIdsByPrincipalName(
                        PRINCIPAL_NAME))
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId());
    }

    @Test
    void shouldFindAuthorizationIdsByRegisteredClientId() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2Authorization first = saveAuthorization(
                client,
                PRINCIPAL_NAME);

        OAuth2Authorization second = saveAuthorization(
                client,
                OTHER_PRINCIPAL_NAME);

        assertThat(queryRepository
                .findIdsByRegisteredClientId(
                        client.getId()))
                .containsExactlyInAnyOrder(
                        first.getId(),
                        second.getId());
    }

    @Test
    void shouldReturnEmptyListWhenNoAuthorizationMatches() {
        assertThat(queryRepository
                .findIdsByPrincipalName(
                        "unknown-principal"))
                .isEmpty();

        assertThat(queryRepository
                .findIdsByRegisteredClientId(
                        "unknown-client"))
                .isEmpty();
    }

    private RegisteredClient saveRegisteredClient() {
        RegisteredClient existing = registeredClientRepository
                .findByClientId(
                        CLIENT_ID);

        if (existing != null) {
            return existing;
        }

        RegisteredClient client = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId(CLIENT_ID)
                .clientIdIssuedAt(ISSUED_AT)
                .clientSecret(passwordEncoder.encode(RAW_CLIENT_SECRET))
                .clientName("R25.11.8 Query Client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT_URI)
                .scope(SCOPE)
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(
                                        false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        Duration.ofMinutes(
                                                15))
                                .refreshTokenTimeToLive(
                                        Duration.ofDays(
                                                30))
                                .reuseRefreshTokens(
                                        false)
                                .build())
                .build();

        registeredClientRepository.save(
                client);

        return client;
    }

    private OAuth2Authorization saveAuthorization(
            RegisteredClient client,
            String principalName) {

        String suffix = UUID.randomUUID()
                .toString();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-" + suffix,
                ISSUED_AT,
                ISSUED_AT.plus(
                        Duration.ofMinutes(
                                15)),
                Set.of(
                        SCOPE));

        OAuth2Authorization authorization = OAuth2Authorization
                .withRegisteredClient(
                        client)
                .id(
                        "authorization-" + suffix)
                .principalName(
                        principalName)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(
                        Set.of(
                                SCOPE))
                .accessToken(
                        accessToken)
                .build();

        authorizationService.save(
                authorization);

        return authorization;
    }
}
