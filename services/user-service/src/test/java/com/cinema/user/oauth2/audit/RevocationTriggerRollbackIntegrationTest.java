package com.cinema.user.oauth2.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserCredential;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.repository.UserCredentialRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserAccountLifecycleService;
import com.cinema.user.service.UserCredentialService;

class RevocationTriggerRollbackIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String CURRENT_PASSWORD = "current-password-123";

    private static final String NEW_PASSWORD = "new-secure-password-456";

    private static final String CURRENT_CLIENT_SECRET = "current-client-secret-123";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    private static final String AUDIT_FAILURE_MESSAGE = "simulated revocation audit failure";

    @Autowired
    private UserAccountLifecycleService accountLifecycleService;

    @Autowired
    private UserCredentialService credentialService;

    @Autowired
    private OAuth2ClientLifecycleService clientLifecycleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Clock clock;

    @MockitoBean
    private RevocationAuditRecorder auditRecorder;

    @BeforeEach
    void failAuditRecording() {
        doThrow(
                new IllegalStateException(
                        AUDIT_FAILURE_MESSAGE))
                .when(
                        auditRecorder)
                .record(
                        any(
                                RevocationAuditTargetType.class),
                        anyString(),
                        any(
                                RevocationReason.class),
                        anyInt());
    }

    @Test
    void accountLockShouldRollbackWhenAuditRecordingFails() {
        Fixture fixture = createUserFixture(
                "account-lock-rollback",
                false);

        assertThatThrownBy(() -> accountLifecycleService.lock(
                fixture.user()
                        .getId()))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        AUDIT_FAILURE_MESSAGE);

        User persistedUser = userRepository.findById(
                fixture.user()
                        .getId())
                .orElseThrow();

        assertThat(persistedUser.getStatus())
                .isEqualTo(
                        AccountStatus.ACTIVE);

        assertThat(persistedUser.getLockedAt())
                .isNull();

        assertAuthorizationActive(
                fixture.authorization()
                        .getId());
    }

    @Test
    void passwordChangeShouldRollbackWhenAuditRecordingFails() {
        Fixture fixture = createUserFixture(
                "password-change-rollback",
                true);

        UserCredential credentialBefore = userCredentialRepository
                .findByUser_Id(
                        fixture.user()
                                .getId())
                .orElseThrow();

        String previousPasswordHash = credentialBefore.getPasswordHash();

        assertThat(passwordEncoder.matches(
                CURRENT_PASSWORD,
                previousPasswordHash))
                .isTrue();

        assertThatThrownBy(() -> credentialService.changePassword(
                fixture.user()
                        .getId(),
                CURRENT_PASSWORD,
                NEW_PASSWORD))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        AUDIT_FAILURE_MESSAGE);

        UserCredential credentialAfter = userCredentialRepository
                .findByUser_Id(
                        fixture.user()
                                .getId())
                .orElseThrow();

        assertThat(credentialAfter.getPasswordHash())
                .isEqualTo(
                        previousPasswordHash);

        assertThat(passwordEncoder.matches(
                CURRENT_PASSWORD,
                credentialAfter.getPasswordHash()))
                .isTrue();

        assertThat(passwordEncoder.matches(
                NEW_PASSWORD,
                credentialAfter.getPasswordHash()))
                .isFalse();

        assertAuthorizationActive(
                fixture.authorization()
                        .getId());
    }

    @Test
    void clientDeactivationShouldRollbackWhenAuditRecordingFails() {
        String suffix = UUID.randomUUID()
                .toString();

        RegisteredClient client = saveRegisteredClient(
                "client-deactivate-rollback",
                suffix);

        OAuth2Authorization authorization = saveAuthorization(
                client,
                "client-rollback-principal-"
                        + suffix
                        + "@example.com",
                "client-deactivate-rollback",
                suffix);

        assertAuthorizationActive(
                authorization.getId());

        assertThatThrownBy(() -> clientLifecycleService.deactivate(
                client.getClientId()))
                .isInstanceOf(
                        IllegalStateException.class)
                .hasMessage(
                        AUDIT_FAILURE_MESSAGE);

        RegisteredClient persistedClient = registeredClientRepository
                .findByClientId(
                        client.getClientId());

        assertThat(persistedClient)
                .isNotNull();

        assertThat(persistedClient.getId())
                .isEqualTo(
                        client.getId());

        assertAuthorizationActive(
                authorization.getId());
    }

    private Fixture createUserFixture(
            String prefix,
            boolean createCredential) {

        String suffix = UUID.randomUUID()
                .toString();

        User user = createActiveUser(
                prefix,
                suffix);

        if (createCredential) {
            credentialService.createCredential(
                    user.getId(),
                    CURRENT_PASSWORD);
        }

        RegisteredClient client = saveRegisteredClient(
                prefix,
                suffix);

        OAuth2Authorization authorization = saveAuthorization(
                client,
                user.getUsername(),
                prefix,
                suffix);

        assertAuthorizationActive(
                authorization.getId());

        return new Fixture(
                user,
                authorization);
    }

    private User createActiveUser(
            String prefix,
            String suffix) {

        String username = prefix
                + "."
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
                        Instant.now(
                                clock)
                                .minusSeconds(
                                        300),
                        ZoneOffset.UTC));

        return userRepository.saveAndFlush(
                user);
    }

    private RegisteredClient saveRegisteredClient(
            String prefix,
            String suffix) {

        Instant issuedAt = Instant.now(
                clock)
                .minusSeconds(
                        120);

        RegisteredClient client = RegisteredClient
                .withId(
                        UUID.randomUUID()
                                .toString())
                .clientId(
                        "r25-11-8-7-3-"
                                + prefix
                                + "-"
                                + suffix)
                .clientIdIssuedAt(
                        issuedAt)
                .clientSecret(
                        passwordEncoder.encode(
                                CURRENT_CLIENT_SECRET))
                .clientName(
                        "R25.11.8.7.3 Rollback Client")
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
            RegisteredClient client,
            String principalName,
            String prefix,
            String suffix) {

        Instant issuedAt = Instant.now(
                clock)
                .minusSeconds(
                        60);

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "rollback-access-"
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
                "rollback-refresh-"
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
                        "rollback-authorization-"
                                + prefix
                                + "-"
                                + suffix)
                .principalName(
                        principalName)
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
            String authorizationId) {

        OAuth2Authorization persisted = authorizationService.findById(
                authorizationId);

        assertThat(persisted)
                .isNotNull();

        assertThat(persisted
                .getAccessToken())
                .isNotNull();

        assertThat(persisted
                .getAccessToken()
                .isInvalidated())
                .isFalse();

        assertThat(persisted
                .getRefreshToken())
                .isNotNull();

        assertThat(persisted
                .getRefreshToken()
                .isInvalidated())
                .isFalse();
    }

    private record Fixture(
            User user,
            OAuth2Authorization authorization) {
    }
}
