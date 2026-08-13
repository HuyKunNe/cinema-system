package com.cinema.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.entity.User;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserAccountLifecycleService;

import jakarta.persistence.EntityManager;

@Transactional
class UserAccountRevocationIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String RAW_CLIENT_SECRET = "r25-11-8-account-revocation-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    @Autowired
    private UserAccountLifecycleService lifecycleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Test
    void lockShouldRevokeUserAuthorizationAndRefreshHistory() {
        Fixture fixture = createFixture(
                "lock");

        lifecycleService.lock(
                fixture.user()
                        .getId());

        flushAndClear();

        User persistedUser = userRepository.findById(
                fixture.user()
                        .getId())
                .orElseThrow();

        assertThat(persistedUser.getStatus())
                .isEqualTo(
                        AccountStatus.LOCKED);

        assertThat(persistedUser.getLockedAt())
                .isNotNull();

        assertAuthorizationRevoked(
                fixture.authorization());
    }

    @Test
    void disableShouldRevokeUserAuthorizationAndRefreshHistory() {
        Fixture fixture = createFixture(
                "disable");

        lifecycleService.disable(
                fixture.user()
                        .getId());

        flushAndClear();

        User persistedUser = userRepository.findById(
                fixture.user()
                        .getId())
                .orElseThrow();

        assertThat(persistedUser.getStatus())
                .isEqualTo(
                        AccountStatus.DISABLED);

        assertThat(persistedUser.getDisabledAt())
                .isNotNull();

        assertAuthorizationRevoked(
                fixture.authorization());
    }

    private Fixture createFixture(
            String prefix) {

        String suffix = UUID.randomUUID()
                .toString();

        User user = createActiveUser(
                prefix,
                suffix);

        RegisteredClient client = saveRegisteredClient(
                prefix,
                suffix);

        OAuth2Authorization authorization = saveAuthorization(
                user,
                client,
                prefix,
                suffix);

        assertAuthorizationActive(
                authorization);

        return new Fixture(
                user,
                authorization);
    }

    private User createActiveUser(
            String prefix,
            String suffix) {

        String username = prefix
                + ".revocation."
                + suffix;

        String email = username
                + "@example.com";

        User user = new User(
                email,
                email.toLowerCase(),
                username,
                username.toLowerCase());

        user.verifyEmail(
                OffsetDateTime.ofInstant(
                        Instant.now(clock)
                                .minusSeconds(
                                        300),
                        ZoneOffset.UTC));

        return userRepository.saveAndFlush(
                user);
    }

    private RegisteredClient saveRegisteredClient(
            String prefix,
            String suffix) {

        Instant issuedAt = Instant.now(clock)
                .minusSeconds(
                        120);

        RegisteredClient client = RegisteredClient
                .withId(
                        UUID.randomUUID()
                                .toString())
                .clientId(
                        "r25-11-8-account-"
                                + prefix
                                + "-"
                                + suffix)
                .clientIdIssuedAt(
                        issuedAt)
                .clientSecret(
                        passwordEncoder.encode(
                                RAW_CLIENT_SECRET))
                .clientName(
                        "R25.11.8 Account Revocation Client")
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(
                        REDIRECT_URI)
                .scope(
                        SCOPE)
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(
                                        false)
                                .requireProofKey(
                                        true)
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
            User user,
            RegisteredClient client,
            String prefix,
            String suffix) {

        Instant issuedAt = Instant.now(clock)
                .minusSeconds(
                        60);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "account-access-"
                        + prefix
                        + "-"
                        + suffix,
                issuedAt,
                issuedAt.plus(
                        Duration.ofMinutes(
                                15)),
                Set.of(
                        SCOPE));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "account-refresh-"
                        + prefix
                        + "-"
                        + suffix,
                issuedAt,
                issuedAt.plus(
                        Duration.ofDays(
                                30)));

        OAuth2Authorization authorization = OAuth2Authorization
                .withRegisteredClient(
                        client)
                .id(
                        "account-authorization-"
                                + prefix
                                + "-"
                                + suffix)
                .principalName(
                        user.getUsername())
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(
                        Set.of(
                                SCOPE))
                .accessToken(
                        accessToken)
                .refreshToken(
                        refreshToken)
                .build();

        authorizationService.save(
                authorization);

        return authorization;
    }

    private void assertAuthorizationActive(
            OAuth2Authorization authorization) {

        OAuth2Authorization persisted = authorizationService.findById(
                authorization.getId());

        assertThat(persisted)
                .isNotNull();

        assertThat(persisted
                .getAccessToken()
                .isInvalidated())
                .isFalse();

        assertThat(persisted
                .getRefreshToken()
                .isInvalidated())
                .isFalse();

        assertThat(refreshTokenHistoryRepository
                .findAllByAuthorizationId(
                        authorization.getId()))
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getStatus())
                            .isEqualTo(
                                    RefreshTokenStatus.ACTIVE);

                    assertThat(history.getRevokedAt())
                            .isNull();
                });
    }

    private void assertAuthorizationRevoked(
            OAuth2Authorization authorization) {

        OAuth2Authorization persisted = authorizationService.findById(
                authorization.getId());

        assertThat(persisted)
                .isNotNull();

        assertThat(persisted
                .getAccessToken()
                .isInvalidated())
                .isTrue();

        assertThat(persisted
                .getRefreshToken()
                .isInvalidated())
                .isTrue();

        List<RefreshTokenHistory> histories = refreshTokenHistoryRepository
                .findAllByAuthorizationId(
                        authorization.getId());

        assertThat(histories)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getStatus())
                            .isEqualTo(
                                    RefreshTokenStatus.REVOKED);

                    assertThat(history.getRevokedAt())
                            .isNotNull();
                });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Fixture(
            User user,
            OAuth2Authorization authorization) {
    }
}
