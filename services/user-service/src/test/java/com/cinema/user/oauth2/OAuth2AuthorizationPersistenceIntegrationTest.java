package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

@Transactional
class OAuth2AuthorizationPersistenceIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String CLIENT_INTERNAL_ID = "019c4000-0000-7000-8000-000000000201";

    private static final String CLIENT_ID = "authorization-persistence-test-client";

    private static final String AUTHORIZATION_ID = "019c4000-0000-7000-8000-000000000202";

    private static final String PRINCIPAL_NAME = "authorization-test-user";

    private static final String AUTHORIZATION_CODE = "authorization-test-code";

    private static final Instant ISSUED_AT = Instant.parse("2026-08-11T03:00:00Z");

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Test
    void shouldUseJdbcAuthorizationService() {
        assertThat(authorizationService)
                .isInstanceOf(
                        JdbcOAuth2AuthorizationService.class);
    }

    @Test
    void shouldUseJdbcAuthorizationConsentService() {
        assertThat(authorizationConsentService)
                .isInstanceOf(
                        JdbcOAuth2AuthorizationConsentService.class);
    }

    @Test
    void shouldPersistAndReloadAuthorization() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2Authorization authorization = createAuthorization(
                client);

        authorizationService.save(authorization);

        OAuth2Authorization found = authorizationService.findById(
                AUTHORIZATION_ID);

        assertThat(found).isNotNull();
        assertThat(found.getId())
                .isEqualTo(AUTHORIZATION_ID);
        assertThat(found.getRegisteredClientId())
                .isEqualTo(CLIENT_INTERNAL_ID);
        assertThat(found.getPrincipalName())
                .isEqualTo(PRINCIPAL_NAME);
        assertThat(found.getAuthorizationGrantType())
                .isEqualTo(
                        AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(found.getAuthorizedScopes())
                .containsExactly("booking:read");

        OAuth2Authorization.Token<OAuth2AuthorizationCode> code = found.getToken(OAuth2AuthorizationCode.class);

        assertThat(code).isNotNull();
        assertThat(code.getToken().getTokenValue())
                .isEqualTo(AUTHORIZATION_CODE);
    }

    @Test
    void shouldFindAuthorizationByCode() {
        RegisteredClient client = saveRegisteredClient();

        authorizationService.save(
                createAuthorization(client));

        OAuth2Authorization found = authorizationService.findByToken(
                AUTHORIZATION_CODE,
                new OAuth2TokenType(
                        OAuth2ParameterNames.CODE));

        assertThat(found).isNotNull();
        assertThat(found.getId())
                .isEqualTo(AUTHORIZATION_ID);
    }

    @Test
    void shouldRemoveAuthorization() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2Authorization authorization = createAuthorization(client);

        authorizationService.save(authorization);
        authorizationService.remove(authorization);

        assertThat(authorizationService.findById(
                AUTHORIZATION_ID))
                .isNull();
    }

    @Test
    void shouldPersistAndReloadAuthorizationConsent() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent
                .withId(
                        client.getId(),
                        PRINCIPAL_NAME)
                .scope("booking:read")
                .build();

        authorizationConsentService.save(consent);

        OAuth2AuthorizationConsent found = authorizationConsentService.findById(
                client.getId(),
                PRINCIPAL_NAME);

        assertThat(found).isNotNull();
        assertThat(found.getRegisteredClientId())
                .isEqualTo(client.getId());
        assertThat(found.getPrincipalName())
                .isEqualTo(PRINCIPAL_NAME);
        assertThat(found.getScopes())
                .containsExactly("booking:read");
    }

    @Test
    void shouldRemoveAuthorizationConsent() {
        RegisteredClient client = saveRegisteredClient();

        OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent
                .withId(
                        client.getId(),
                        PRINCIPAL_NAME)
                .scope("booking:read")
                .build();

        authorizationConsentService.save(consent);
        authorizationConsentService.remove(consent);

        assertThat(authorizationConsentService.findById(
                client.getId(),
                PRINCIPAL_NAME))
                .isNull();
    }

    private RegisteredClient saveRegisteredClient() {
        RegisteredClient registeredClient = RegisteredClient
                .withId(CLIENT_INTERNAL_ID)
                .clientId(CLIENT_ID)
                .clientIdIssuedAt(ISSUED_AT)
                .clientName(
                        "Authorization Persistence Test Client")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "http://127.0.0.1:3000/callback")
                .scope("booking:read")
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(true)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        Duration.ofMinutes(15))
                                .build())
                .build();

        registeredClientRepository.save(
                registeredClient);

        return registeredClient;
    }

    private OAuth2Authorization createAuthorization(
            RegisteredClient client) {

        OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                AUTHORIZATION_CODE,
                ISSUED_AT,
                ISSUED_AT.plus(5, ChronoUnit.MINUTES));

        return OAuth2Authorization
                .withRegisteredClient(client)
                .id(AUTHORIZATION_ID)
                .principalName(PRINCIPAL_NAME)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(
                        Set.of("booking:read"))
                .token(authorizationCode)
                .build();
    }
}
