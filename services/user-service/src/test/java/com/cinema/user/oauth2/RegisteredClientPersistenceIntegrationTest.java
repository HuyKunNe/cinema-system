package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

@Transactional
class RegisteredClientPersistenceIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String PUBLIC_CLIENT_ID = "019c4000-0000-7000-8000-000000000101";

    private static final String CONFIDENTIAL_CLIENT_ID = "019c4000-0000-7000-8000-000000000102";

    private static final String DUPLICATE_FIRST_ID = "019c4000-0000-7000-8000-000000000103";

    private static final String DUPLICATE_SECOND_ID = "019c4000-0000-7000-8000-000000000104";

    private static final Instant ISSUED_AT = Instant.parse("2026-08-10T03:00:00Z");

    private static final String RAW_CLIENT_SECRET = "local-service-secret-for-testing-only";

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistPublicPkceClient() {
        RegisteredClient client = publicClient();

        registeredClientRepository.save(client);

        RegisteredClient found = registeredClientRepository
                .findByClientId("cinema-web");

        assertThat(found).isNotNull();
        assertThat(found.getId())
                .isEqualTo(PUBLIC_CLIENT_ID);

        assertThat(UUID.fromString(found.getId()).version())
                .isEqualTo(7);

        assertThat(found.getClientSecret()).isNull();

        assertThat(found.getClientAuthenticationMethods())
                .containsExactly(
                        ClientAuthenticationMethod.NONE);

        assertThat(found.getAuthorizationGrantTypes())
                .containsOnly(
                        AuthorizationGrantType.AUTHORIZATION_CODE);

        assertThat(found.getRedirectUris())
                .containsExactly(
                        "http://127.0.0.1:3000/callback");

        assertThat(found.getPostLogoutRedirectUris())
                .containsExactly(
                        "http://127.0.0.1:3000");

        assertThat(found.getScopes())
                .containsExactlyInAnyOrder(
                        OidcScopes.OPENID,
                        OidcScopes.PROFILE,
                        "booking:read",
                        "booking:create");

        assertThat(found.getClientSettings()
                .isRequireProofKey())
                .isTrue();

        assertThat(found.getClientSettings()
                .isRequireAuthorizationConsent())
                .isTrue();

        assertThat(found.getTokenSettings()
                .getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(15));

        assertThat(found.getTokenSettings()
                .getRefreshTokenTimeToLive())
                .isEqualTo(Duration.ofDays(30));

        assertThat(found.getTokenSettings()
                .isReuseRefreshTokens())
                .isFalse();
    }

    @Test
    void shouldPersistConfidentialServiceClientWithEncodedSecret() {
        String encodedSecret = passwordEncoder.encode(
                RAW_CLIENT_SECRET);

        RegisteredClient client = RegisteredClient
                .withId(CONFIDENTIAL_CLIENT_ID)
                .clientId("inventory-service")
                .clientIdIssuedAt(ISSUED_AT)
                .clientSecret(encodedSecret)
                .clientName("Inventory Service")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("inventory:write")
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        Duration.ofMinutes(5))
                                .build())
                .build();

        registeredClientRepository.save(client);

        RegisteredClient found = registeredClientRepository
                .findByClientId("inventory-service");

        assertThat(found).isNotNull();

        assertThat(found.getClientSecret())
                .isNotEqualTo(RAW_CLIENT_SECRET)
                .startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(
                RAW_CLIENT_SECRET,
                found.getClientSecret()))
                .isTrue();

        assertThat(found.getClientAuthenticationMethods())
                .containsExactly(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        assertThat(found.getAuthorizationGrantTypes())
                .containsExactly(
                        AuthorizationGrantType.CLIENT_CREDENTIALS);

        assertThat(found.getScopes())
                .containsExactly("inventory:write");

        assertThat(found.getTokenSettings()
                .getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void shouldFindRegisteredClientByInternalId() {
        registeredClientRepository.save(publicClient());

        RegisteredClient found = registeredClientRepository
                .findById(PUBLIC_CLIENT_ID);

        assertThat(found).isNotNull();
        assertThat(found.getClientId())
                .isEqualTo("cinema-web");
    }

    @Test
    void shouldRejectDuplicateClientId() {
        RegisteredClient first = serviceClient(
                DUPLICATE_FIRST_ID,
                "duplicate-service");

        RegisteredClient second = serviceClient(
                DUPLICATE_SECOND_ID,
                "duplicate-service");

        registeredClientRepository.save(first);

        assertThatThrownBy(() -> registeredClientRepository.save(second))
                .isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessageContaining(
                        "Registered client must be unique");
    }

    @Test
    void clientIdShouldBeCaseSensitive() {
        registeredClientRepository.save(
                serviceClient(
                        DUPLICATE_FIRST_ID,
                        "cinema-service"));

        registeredClientRepository.save(
                serviceClient(
                        DUPLICATE_SECOND_ID,
                        "CINEMA-SERVICE"));

        RegisteredClient lowercase = registeredClientRepository
                .findByClientId("cinema-service");

        RegisteredClient uppercase = registeredClientRepository
                .findByClientId("CINEMA-SERVICE");

        assertThat(lowercase).isNotNull();
        assertThat(uppercase).isNotNull();

        assertThat(lowercase.getId())
                .isEqualTo(DUPLICATE_FIRST_ID);

        assertThat(uppercase.getId())
                .isEqualTo(DUPLICATE_SECOND_ID);
    }

    private RegisteredClient publicClient() {
        return RegisteredClient
                .withId(PUBLIC_CLIENT_ID)
                .clientId("cinema-web")
                .clientIdIssuedAt(ISSUED_AT)
                .clientName("Cinema Web")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(
                        "http://127.0.0.1:3000/callback")
                .postLogoutRedirectUri(
                        "http://127.0.0.1:3000")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("booking:read")
                .scope("booking:create")
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(true)
                                .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .build())
                .build();
    }

    private RegisteredClient serviceClient(
            String id,
            String clientId) {

        return RegisteredClient
                .withId(id)
                .clientId(clientId)
                .clientIdIssuedAt(ISSUED_AT)
                .clientSecret(
                        passwordEncoder.encode(
                                "test-service-secret"))
                .clientName(clientId)
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("service:access")
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        Duration.ofMinutes(5))
                                .build())
                .build();
    }

    private static TokenSettings defaultTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(
                        Duration.ofMinutes(15))
                .refreshTokenTimeToLive(
                        Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .build();
    }
}
