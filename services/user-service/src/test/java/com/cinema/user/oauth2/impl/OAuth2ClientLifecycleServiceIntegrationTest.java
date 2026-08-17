package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
class OAuth2ClientLifecycleServiceIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CURRENT_RAW_SECRET = "current-client-lifecycle-secret";

    private static final String NEW_RAW_SECRET = "new-client-lifecycle-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    @Autowired private OAuth2ClientLifecycleService clientLifecycleService;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private OAuth2AuthorizationService authorizationService;

    @Autowired private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private Clock clock;

    @Test
    void deactivateShouldRevokeAuthorizationAndHideClient() {
        Fixture fixture = createConfidentialFixture("deactivate");

        clientLifecycleService.deactivate(fixture.client().getClientId());

        assertThat(registeredClientRepository.findById(fixture.client().getId())).isNull();

        assertThat(registeredClientRepository.findByClientId(fixture.client().getClientId()))
                .isNull();

        Boolean active =
                jdbcTemplate.queryForObject(
                        """
                        SELECT active
                        FROM oauth2_registered_client
                        WHERE id = ?
                        """,
                        Boolean.class,
                        fixture.client().getId());

        assertThat(active).isFalse();

        assertAuthorizationMetadataInvalidated(fixture.authorization().getId());

        assertHistoryRevoked(fixture.authorization().getId());

        assertLifecycleAudit(
                fixture.client().getClientId(), SecurityAuditEventType.OAUTH2_CLIENT_DEACTIVATED);
    }

    @Test
    void rotateSecretShouldReplaceSecretAndRevokeAuthorization() {
        Fixture fixture = createConfidentialFixture("rotate");

        String previousHash = fixture.client().getClientSecret();

        clientLifecycleService.rotateSecret(fixture.client().getClientId(), NEW_RAW_SECRET);

        RegisteredClient reloaded =
                registeredClientRepository.findByClientId(fixture.client().getClientId());

        assertThat(reloaded).isNotNull();

        assertThat(reloaded.getClientSecret()).isNotEqualTo(previousHash).startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(CURRENT_RAW_SECRET, reloaded.getClientSecret()))
                .isFalse();

        assertThat(passwordEncoder.matches(NEW_RAW_SECRET, reloaded.getClientSecret())).isTrue();

        assertAuthorizationRevoked(fixture.authorization().getId());

        assertHistoryRevoked(fixture.authorization().getId());

        assertLifecycleAudit(
                fixture.client().getClientId(),
                SecurityAuditEventType.OAUTH2_CLIENT_SECRET_ROTATED);

        List<SecurityAuditEvent> auditEvents =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.OAUTH2_CLIENT,
                                fixture.client().getClientId());

        assertThat(auditEvents)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getReason()).isNull();

                            assertThat(event.getMetadata()).isNull();

                            assertThat(event.getTargetReference())
                                    .doesNotContain(
                                            CURRENT_RAW_SECRET,
                                            NEW_RAW_SECRET,
                                            previousHash,
                                            reloaded.getClientSecret());
                        });
    }

    @Test
    void rejectedPublicClientRotationShouldNotMutateClient() {
        RegisteredClient client = savePublicClient();

        assertThatThrownBy(
                        () ->
                                clientLifecycleService.rotateSecret(
                                        client.getClientId(), NEW_RAW_SECRET))
                .isInstanceOf(ConflictException.class);

        RegisteredClient persisted =
                registeredClientRepository.findByClientId(client.getClientId());

        assertThat(persisted).isNotNull();

        assertThat(persisted.getClientSecret()).isNull();

        Boolean active =
                jdbcTemplate.queryForObject(
                        """
                        SELECT active
                        FROM oauth2_registered_client
                        WHERE id = ?
                        """,
                        Boolean.class,
                        client.getId());

        assertThat(active).isTrue();

        assertThat(
                        securityAuditEventRepository
                                .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                        SecurityAuditTargetType.OAUTH2_CLIENT,
                                        client.getClientId()))
                .isEmpty();
    }

    private void assertLifecycleAudit(String clientId, SecurityAuditEventType expectedEventType) {

        List<SecurityAuditEvent> events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.OAUTH2_CLIENT, clientId);

        assertThat(events)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType()).isEqualTo(expectedEventType);

                            assertThat(event.getActorType())
                                    .isEqualTo(SecurityAuditActorType.SYSTEM);

                            assertThat(event.getActorReference()).isEqualTo(CommonConstants.SYSTEM);

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.OAUTH2_CLIENT);

                            assertThat(event.getTargetReference()).isEqualTo(clientId);

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getReason()).isNull();

                            assertThat(event.getMetadata()).isNull();

                            assertThat(event.getOccurredAt()).isNotNull();

                            assertThat(event.getCreatedAt()).isNotNull();

                            assertThat(event.getUpdatedAt()).isNotNull();
                        });
    }

    private Fixture createConfidentialFixture(String prefix) {

        String suffix = UUID.randomUUID().toString();

        RegisteredClient client = saveConfidentialClient(prefix, suffix);

        OAuth2Authorization authorization = saveAuthorization(client, prefix, suffix);

        assertAuthorizationActive(authorization.getId());

        return new Fixture(client, authorization);
    }

    private RegisteredClient saveConfidentialClient(String prefix, String suffix) {

        Instant issuedAt = Instant.now(clock).minusSeconds(60);

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("r25-11-8-5-" + prefix + "-" + suffix)
                        .clientIdIssuedAt(issuedAt)
                        .clientSecret(passwordEncoder.encode(CURRENT_RAW_SECRET))
                        .clientName("R25.11.8.5 Client Lifecycle")
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

    private RegisteredClient savePublicClient() {
        String suffix = UUID.randomUUID().toString();

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                        .clientId("r25-11-8-5-public-" + suffix)
                        .clientIdIssuedAt(Instant.now(clock))
                        .clientName("R25.11.8.5 Public Client")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("http://127.0.0.1:3000/callback")
                        .scope("openid")
                        .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                        .build();

        registeredClientRepository.save(client);

        return client;
    }

    private OAuth2Authorization saveAuthorization(
            RegisteredClient client, String prefix, String suffix) {

        Instant issuedAt = Instant.now(clock).minusSeconds(30);

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "lifecycle-access-" + prefix + "-" + suffix,
                        issuedAt,
                        issuedAt.plus(Duration.ofMinutes(15)),
                        Set.of(SCOPE));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        "lifecycle-refresh-" + prefix + "-" + suffix,
                        issuedAt,
                        issuedAt.plus(Duration.ofDays(30)));

        OAuth2Authorization authorization =
                OAuth2Authorization.withRegisteredClient(client)
                        .id("lifecycle-authorization-" + prefix + "-" + suffix)
                        .principalName("lifecycle-user-" + suffix)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizedScopes(Set.of(SCOPE))
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

        authorizationService.save(authorization);

        return authorization;
    }

    private void assertAuthorizationActive(String authorizationId) {

        OAuth2Authorization authorization = authorizationService.findById(authorizationId);

        assertThat(authorization).isNotNull();

        assertThat(authorization.getAccessToken().isInvalidated()).isFalse();

        assertThat(authorization.getRefreshToken().isInvalidated()).isFalse();

        assertThat(refreshTokenHistoryRepository.findAllByAuthorizationId(authorizationId))
                .singleElement()
                .satisfies(
                        history -> {
                            assertThat(history.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

                            assertThat(history.getRevokedAt()).isNull();
                        });
    }

    private void assertAuthorizationRevoked(String authorizationId) {

        OAuth2Authorization authorization = authorizationService.findById(authorizationId);

        assertThat(authorization).isNotNull();

        assertThat(authorization.getAccessToken().isInvalidated()).isTrue();

        assertThat(authorization.getRefreshToken().isInvalidated()).isTrue();
    }

    private void assertAuthorizationMetadataInvalidated(String authorizationId) {

        String accessMetadata =
                jdbcTemplate.queryForObject(
                        """
                        SELECT access_token_metadata
                        FROM oauth2_authorization
                        WHERE id = ?
                        """,
                        String.class,
                        authorizationId);

        String refreshMetadata =
                jdbcTemplate.queryForObject(
                        """
                        SELECT refresh_token_metadata
                        FROM oauth2_authorization
                        WHERE id = ?
                        """,
                        String.class,
                        authorizationId);

        assertThat(accessMetadata).contains("metadata.token.invalidated").contains("true");

        assertThat(refreshMetadata).contains("metadata.token.invalidated").contains("true");
    }

    private void assertHistoryRevoked(String authorizationId) {

        assertThat(refreshTokenHistoryRepository.findAllByAuthorizationId(authorizationId))
                .singleElement()
                .satisfies(
                        history -> {
                            assertThat(history.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

                            assertThat(history.getRevokedAt()).isNotNull();
                        });
    }

    private record Fixture(RegisteredClient client, OAuth2Authorization authorization) {}
}
