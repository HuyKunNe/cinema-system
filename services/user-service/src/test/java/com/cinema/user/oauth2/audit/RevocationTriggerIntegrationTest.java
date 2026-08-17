package com.cinema.user.oauth2.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RevocationAuditEvent;
import com.cinema.user.entity.User;
import com.cinema.user.oauth2.AdministrativeAuthorizationService;
import com.cinema.user.oauth2.OAuth2ClientLifecycleService;
import com.cinema.user.repository.RevocationAuditEventRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.service.UserAccountLifecycleService;
import com.cinema.user.service.UserCredentialService;

import jakarta.persistence.EntityManager;

@Transactional
@WithMockUser(username = "revocation-audit-admin", authorities = "user:manage")
class RevocationTriggerIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String CURRENT_PASSWORD = "current-password-123";

    private static final String NEW_PASSWORD = "new-secure-password-456";

    private static final String CURRENT_CLIENT_SECRET = "current-client-secret-123";

    private static final String NEW_CLIENT_SECRET = "new-client-secret-456";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    private static final String ACTOR_NAME = "revocation-audit-admin";

    @Autowired
    private UserAccountLifecycleService accountLifecycleService;

    @Autowired
    private UserCredentialService credentialService;

    @Autowired
    private OAuth2ClientLifecycleService clientLifecycleService;

    @Autowired
    private AdministrativeAuthorizationService administrativeAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private RevocationAuditEventRepository auditEventRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @Test
    void accountLockShouldRecordAccountLockedReason() {
        User user = createActiveUser(
                "account-lock");

        accountLifecycleService.lock(
                user.getId());

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.USER,
                user.getUsername(),
                RevocationReason.ACCOUNT_LOCKED);
    }

    @Test
    void accountDisableShouldRecordAccountDisabledReason() {
        User user = createActiveUser(
                "account-disable");

        accountLifecycleService.disable(
                user.getId());

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.USER,
                user.getUsername(),
                RevocationReason.ACCOUNT_DISABLED);
    }

    @Test
    void passwordChangeShouldRecordPasswordChangedReason() {
        User user = createActiveUser(
                "password-change");

        credentialService.createCredential(
                user.getId(),
                CURRENT_PASSWORD);

        credentialService.changePassword(
                user.getId(),
                CURRENT_PASSWORD,
                NEW_PASSWORD);

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.USER,
                user.getUsername(),
                RevocationReason.PASSWORD_CHANGED);
    }

    @Test
    void passwordResetShouldRecordPasswordResetReason() {
        User user = createActiveUser(
                "password-reset");

        credentialService.createCredential(
                user.getId(),
                CURRENT_PASSWORD);

        credentialService.resetPassword(
                user.getId(),
                NEW_PASSWORD);

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.USER,
                user.getUsername(),
                RevocationReason.PASSWORD_RESET);
    }

    @Test
    void clientDeactivationShouldRecordClientDeactivatedReason() {
        RegisteredClient client = createConfidentialClient(
                "client-deactivate");

        clientLifecycleService.deactivate(
                client.getClientId());

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.CLIENT,
                client.getClientId(),
                RevocationReason.CLIENT_DEACTIVATED);
    }

    @Test
    void clientSecretRotationShouldRecordClientSecretRotatedReason() {
        RegisteredClient client = createConfidentialClient(
                "client-secret-rotation");

        clientLifecycleService.rotateSecret(
                client.getClientId(),
                NEW_CLIENT_SECRET);

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.CLIENT,
                client.getClientId(),
                RevocationReason.CLIENT_SECRET_ROTATED);
    }

    @Test
    void administrativeUserRevocationShouldRecordAdminUserReason() {
        User user = createActiveUser(
                "admin-user-revocation");

        administrativeAuthorizationService.revokeUserAuthorizations(
                user.getId());

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.USER,
                user.getUsername(),
                RevocationReason.ADMIN_USER_REVOCATION);
    }

    @Test
    void administrativeClientRevocationShouldRecordAdminClientReason() {
        RegisteredClient client = createConfidentialClient(
                "admin-client-revocation");

        administrativeAuthorizationService.revokeClientAuthorizations(
                client.getClientId());

        flushAndClear();

        assertSingleAuditEvent(
                RevocationAuditTargetType.CLIENT,
                client.getClientId(),
                RevocationReason.ADMIN_CLIENT_REVOCATION);
    }

    private User createActiveUser(
            String prefix) {

        String suffix = UUID.randomUUID()
                .toString();

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

    private RegisteredClient createConfidentialClient(
            String prefix) {

        String suffix = UUID.randomUUID()
                .toString();

        Instant issuedAt = Instant.now(
                clock)
                .minusSeconds(
                        60);

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
                        "R25.11.8.7.3 Revocation Audit Client")
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
                                .reuseRefreshTokens(
                                        false)
                                .build())
                .build();

        registeredClientRepository.save(
                client);

        return client;
    }

    private void assertSingleAuditEvent(
            RevocationAuditTargetType targetType,
            String targetReference,
            RevocationReason expectedReason) {

        List<RevocationAuditEvent> events = auditEventRepository
                .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                        targetType,
                        targetReference);

        assertThat(events)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getTargetType())
                            .isEqualTo(
                                    targetType);

                    assertThat(event.getTargetReference())
                            .isEqualTo(
                                    targetReference);

                    assertThat(event.getReason())
                            .isEqualTo(
                                    expectedReason);

                    assertThat(event.getActorUserId())
                            .isNull();

                    assertThat(event.getActorName())
                            .isEqualTo(
                                    ACTOR_NAME);

                    assertThat(event.getRevokedAuthorizationCount())
                            .isZero();

                    assertThat(event.getOccurredAt())
                            .isNotNull();

                    assertThat(event.getCreatedAt())
                            .isNotNull();

                    assertThat(event.getUpdatedAt())
                            .isNotNull();
                });
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
