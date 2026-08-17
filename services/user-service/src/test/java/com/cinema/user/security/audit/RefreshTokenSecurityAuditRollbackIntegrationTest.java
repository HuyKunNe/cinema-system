package com.cinema.user.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.oauth2.token.RefreshTokenHasher;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

class RefreshTokenSecurityAuditRollbackIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String FIRST_REFRESH_TOKEN = "rollback-first-refresh-token";

    private static final String SECOND_REFRESH_TOKEN = "rollback-second-refresh-token";

    private static final String AUDIT_FAILURE_MESSAGE = "simulated durable security audit failure";

    private static final String SCOPE = "booking:read";

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private OAuth2AuthorizationService authorizationService;

    @Autowired private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired private RefreshTokenHasher refreshTokenHasher;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private Clock clock;

    @MockitoBean private SecurityAuditRecorder securityAuditRecorder;

    @BeforeEach
    void failSecurityAuditRecording() {
        doThrow(new IllegalStateException(AUDIT_FAILURE_MESSAGE))
                .when(securityAuditRecorder)
                .record(any(SecurityAuditRecord.class));
    }

    @Test
    void refreshTokenReuseShouldRollbackWhenAuditFails() {
        Fixture fixture = createRotatedTokenFixture();

        RefreshTokenHistory rotatedBefore = findHistory(FIRST_REFRESH_TOKEN);

        RefreshTokenHistory activeBefore = findHistory(SECOND_REFRESH_TOKEN);

        assertThat(rotatedBefore.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);

        assertThat(rotatedBefore.getReusedAt()).isNull();

        assertThat(activeBefore.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        assertThat(activeBefore.getRevokedAt()).isNull();

        assertAuthorizationActive(fixture.authorizationId());

        assertThatThrownBy(
                        () ->
                                authorizationService.findByToken(
                                        FIRST_REFRESH_TOKEN, OAuth2TokenType.REFRESH_TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(AUDIT_FAILURE_MESSAGE);

        RefreshTokenHistory rotatedAfter = findHistory(FIRST_REFRESH_TOKEN);

        RefreshTokenHistory activeAfter = findHistory(SECOND_REFRESH_TOKEN);

        assertThat(rotatedAfter.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);

        assertThat(rotatedAfter.getReusedAt()).isNull();

        assertThat(activeAfter.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        assertThat(activeAfter.getRevokedAt()).isNull();

        assertAuthorizationActive(fixture.authorizationId());
    }

    private Fixture createRotatedTokenFixture() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(
                status -> {
                    RegisteredClient client = saveClient();

                    OAuth2Authorization initial = initialAuthorization(client);

                    authorizationService.save(initial);

                    OAuth2Authorization rotated = rotatedAuthorization(initial);

                    authorizationService.save(rotated);

                    return new Fixture(rotated.getId());
                });
    }

    private RegisteredClient saveClient() {
        String suffix = UUID.randomUUID().toString();

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("refresh-audit-rollback-" + suffix)
                        .clientIdIssuedAt(Instant.now(clock))
                        .clientSecret("{noop}rollback-secret")
                        .clientName("Refresh Audit Rollback Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://127.0.0.1:8080" + "/login/oauth2/code/cinema")
                        .scope(SCOPE)
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

    private OAuth2Authorization initialAuthorization(RegisteredClient client) {

        Instant issuedAt = Instant.now(clock).minusSeconds(60);

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "rollback-initial-access-token",
                        issuedAt,
                        issuedAt.plus(Duration.ofMinutes(15)),
                        Set.of(SCOPE));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        FIRST_REFRESH_TOKEN, issuedAt, issuedAt.plus(Duration.ofDays(30)));

        return OAuth2Authorization.withRegisteredClient(client)
                .id("refresh-audit-rollback-authorization-" + UUID.randomUUID())
                .principalName("refresh-audit-rollback-user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of(SCOPE))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private OAuth2Authorization rotatedAuthorization(OAuth2Authorization initial) {

        Instant issuedAt = Instant.now(clock);

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "rollback-rotated-access-token",
                        issuedAt,
                        issuedAt.plus(Duration.ofMinutes(15)),
                        Set.of(SCOPE));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        SECOND_REFRESH_TOKEN, issuedAt, issuedAt.plus(Duration.ofDays(30)));

        return OAuth2Authorization.from(initial)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private RefreshTokenHistory findHistory(String rawToken) {

        return refreshTokenHistoryRepository
                .findByTokenHash(refreshTokenHasher.hash(rawToken))
                .orElseThrow();
    }

    private void assertAuthorizationActive(String authorizationId) {

        OAuth2Authorization authorization = authorizationService.findById(authorizationId);

        assertThat(authorization).isNotNull();

        assertThat(authorization.getAccessToken()).isNotNull();

        assertThat(authorization.getAccessToken().isInvalidated()).isFalse();

        assertThat(authorization.getRefreshToken()).isNotNull();

        assertThat(authorization.getRefreshToken().isInvalidated()).isFalse();

        assertThat(authorization.getRefreshToken().getToken().getTokenValue())
                .isEqualTo(SECOND_REFRESH_TOKEN);
    }

    private record Fixture(String authorizationId) {}
}
