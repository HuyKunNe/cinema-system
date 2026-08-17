package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.audit.RevocationReason;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Transactional
class AuthorizationSessionRevocationServiceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String RAW_CLIENT_SECRET = "r25-11-8-revocation-client-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    @Autowired private AuthorizationSessionRevocationService revocationService;

    @Autowired private OAuth2AuthorizationService authorizationService;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private Clock clock;

    @Autowired private EntityManager entityManager;

    @Test
    void shouldRevokeAuthorizationAndHistoryByPrincipalName() {
        String suffix = UUID.randomUUID().toString();

        String principalName = "principal-revocation-" + suffix + "@example.com";

        RegisteredClient client = saveRegisteredClient(suffix);

        OAuth2Authorization authorization = saveAuthorization(client, principalName, suffix);

        assertActive(authorization);

        revocationService.revokeByPrincipalName(
                principalName, RevocationReason.ADMIN_USER_REVOCATION);

        flushAndClear();

        assertRevoked(authorization);
    }

    @Test
    void shouldRevokeAuthorizationAndHistoryByRegisteredClientId() {
        String suffix = UUID.randomUUID().toString();

        String principalName = "client-revocation-" + suffix + "@example.com";

        RegisteredClient client = saveRegisteredClient(suffix);

        OAuth2Authorization authorization = saveAuthorization(client, principalName, suffix);

        assertActive(authorization);

        revocationService.revokeByRegisteredClientId(
                client.getId(), client.getClientId(), RevocationReason.ADMIN_CLIENT_REVOCATION);

        flushAndClear();

        assertRevoked(authorization);
    }

    @Test
    void repeatedRevocationShouldRemainIdempotent() {
        String suffix = UUID.randomUUID().toString();

        String principalName = "idempotent-revocation-" + suffix + "@example.com";

        RegisteredClient client = saveRegisteredClient(suffix);

        OAuth2Authorization authorization = saveAuthorization(client, principalName, suffix);

        revocationService.revokeByPrincipalName(
                principalName, RevocationReason.ADMIN_USER_REVOCATION);

        revocationService.revokeByPrincipalName(
                principalName, RevocationReason.ADMIN_USER_REVOCATION);

        flushAndClear();

        assertRevoked(authorization);

        assertThat(refreshTokenHistoryRepository.findAllByAuthorizationId(authorization.getId()))
                .singleElement()
                .satisfies(
                        history -> {
                            assertThat(history.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

                            assertThat(history.getRevokedAt()).isNotNull();
                        });
    }

    private RegisteredClient saveRegisteredClient(String suffix) {

        String clientId = "r25-11-8-revocation-" + suffix;

        Instant issuedAt = Instant.now(clock).minusSeconds(60);

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId(clientId)
                        .clientIdIssuedAt(issuedAt)
                        .clientSecret(passwordEncoder.encode(RAW_CLIENT_SECRET))
                        .clientName("R25.11.8 Revocation Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri(REDIRECT_URI)
                        .scope(SCOPE)
                        .clientSettings(
                                ClientSettings.builder()
                                        .requireAuthorizationConsent(false)
                                        .requireProofKey(true)
                                        .build())
                        .tokenSettings(
                                TokenSettings.builder()
                                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                                        .refreshTokenTimeToLive(Duration.ofDays(30))
                                        .reuseRefreshTokens(false)
                                        .build())
                        .build();

        registeredClientRepository.save(client);

        return client;
    }

    private OAuth2Authorization saveAuthorization(
            RegisteredClient client, String principalName, String suffix) {

        Instant issuedAt = Instant.now(clock).minusSeconds(30);

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "access-token-" + suffix,
                        issuedAt,
                        issuedAt.plus(Duration.ofMinutes(15)),
                        Set.of(SCOPE));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        "refresh-token-" + suffix, issuedAt, issuedAt.plus(Duration.ofDays(30)));

        OAuth2Authorization authorization =
                OAuth2Authorization.withRegisteredClient(client)
                        .id("authorization-" + suffix)
                        .principalName(principalName)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizedScopes(Set.of(SCOPE))
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

        authorizationService.save(authorization);

        return authorization;
    }

    private void assertActive(OAuth2Authorization authorization) {

        OAuth2Authorization persisted = authorizationService.findById(authorization.getId());

        assertThat(persisted).isNotNull();

        assertThat(persisted.getAccessToken().isInvalidated()).isFalse();

        assertThat(persisted.getRefreshToken().isInvalidated()).isFalse();

        assertThat(refreshTokenHistoryRepository.findAllByAuthorizationId(authorization.getId()))
                .singleElement()
                .satisfies(
                        history -> {
                            assertThat(history.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

                            assertThat(history.getRevokedAt()).isNull();
                        });
    }

    private void assertRevoked(OAuth2Authorization authorization) {

        OAuth2Authorization persisted = authorizationService.findById(authorization.getId());

        assertThat(persisted).isNotNull();

        assertThat(persisted.getAccessToken()).isNotNull();

        assertThat(persisted.getAccessToken().isInvalidated()).isTrue();

        assertThat(persisted.getRefreshToken()).isNotNull();

        assertThat(persisted.getRefreshToken().isInvalidated()).isTrue();

        List<RefreshTokenHistory> histories =
                refreshTokenHistoryRepository.findAllByAuthorizationId(authorization.getId());

        assertThat(histories)
                .singleElement()
                .satisfies(
                        history -> {
                            assertThat(history.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

                            assertThat(history.getRevokedAt()).isNotNull();
                        });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
