package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.exception.exception.ConflictException;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.RegisteredClientRegistrationResult;
import com.cinema.user.oauth2.model.ServiceClientRegistration;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;

@Transactional
class OAuth2ClientRegistrationServiceIntegrationTest extends AbstractMySqlIntegrationTest {

    @Autowired private OAuth2ClientRegistrationService registrationService;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

    @Test
    void shouldRegisterPublicPkceClient() {
        PublicClientRegistration registration =
                new PublicClientRegistration(
                        "integration-cinema-web",
                        "Integration Cinema Web",
                        Set.of("http://127.0.0.1:3000/callback"),
                        Set.of("http://127.0.0.1:3000"),
                        Set.of(
                                OidcScopes.OPENID,
                                OidcScopes.PROFILE,
                                "booking:read",
                                "booking:create"));

        RegisteredClientRegistrationResult result =
                registrationService.registerPublicClient(registration);

        RegisteredClient persisted =
                registeredClientRepository.findByClientId("integration-cinema-web");

        assertThat(persisted).isNotNull();

        assertThat(result.id()).isEqualTo(persisted.getId());

        assertThat(UUID.fromString(persisted.getId()).version()).isEqualTo(7);

        assertThat(persisted.getClientSecret()).isNull();

        assertThat(persisted.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);

        assertThat(persisted.getAuthorizationGrantTypes())
                .containsOnly(AuthorizationGrantType.AUTHORIZATION_CODE);

        assertThat(persisted.getClientSettings().isRequireProofKey()).isTrue();

        assertThat(persisted.getClientSettings().isRequireAuthorizationConsent()).isTrue();

        assertRegistrationAudit("integration-cinema-web", "clientType=PUBLIC", null, null);
    }

    @Test
    void shouldRegisterServiceClientWithEncodedSecret() {
        String rawSecret = "integration-service-secret";

        ServiceClientRegistration registration =
                new ServiceClientRegistration(
                        "integration-inventory-service",
                        "Integration Inventory Service",
                        rawSecret,
                        Set.of("inventory:write"));

        RegisteredClientRegistrationResult result =
                registrationService.registerServiceClient(registration);

        RegisteredClient persisted =
                registeredClientRepository.findByClientId("integration-inventory-service");

        assertThat(persisted).isNotNull();

        assertThat(result.id()).isEqualTo(persisted.getId());

        assertThat(persisted.getClientSecret()).isNotEqualTo(rawSecret).startsWith("{bcrypt}");

        assertThat(passwordEncoder.matches(rawSecret, persisted.getClientSecret())).isTrue();

        assertThat(persisted.getAuthorizationGrantTypes())
                .containsExactly(AuthorizationGrantType.CLIENT_CREDENTIALS);

        assertThat(persisted.getRedirectUris()).isEmpty();

        assertThat(persisted.getPostLogoutRedirectUris()).isEmpty();

        assertThat(persisted.getScopes()).containsExactly("inventory:write");

        assertRegistrationAudit(
                "integration-inventory-service",
                "clientType=SERVICE",
                rawSecret,
                persisted.getClientSecret());
    }

    @Test
    void shouldRejectDuplicateClientIdWithConflictException() {
        PublicClientRegistration registration =
                new PublicClientRegistration(
                        "integration-duplicate-web",
                        "Integration Duplicate Web",
                        Set.of("http://127.0.0.1:3000/callback"),
                        Set.of(),
                        Set.of(OidcScopes.OPENID));

        registrationService.registerPublicClient(registration);

        assertThatThrownBy(() -> registrationService.registerPublicClient(registration))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void registrationResultShouldNotExposeServiceSecret() {
        String rawSecret = "result-secret-must-not-leak";

        RegisteredClientRegistrationResult result =
                registrationService.registerServiceClient(
                        new ServiceClientRegistration(
                                "integration-safe-result",
                                "Integration Safe Result",
                                rawSecret,
                                Set.of("service:access")));

        RegisteredClient persisted =
                registeredClientRepository.findByClientId("integration-safe-result");

        assertThat(result.toString())
                .doesNotContain(rawSecret)
                .doesNotContain(persisted.getClientSecret());
    }

    @ParameterizedTest
    @ValueSource(strings = {OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL})
    void shouldRejectHumanOidcScopeForServiceClient(String scope) {

        ServiceClientRegistration registration =
                new ServiceClientRegistration(
                        "integration-invalid-scope-" + scope,
                        "Invalid Scope Service",
                        "integration-secret",
                        Set.of(scope));

        assertThatThrownBy(() -> registrationService.registerServiceClient(registration))
                .isInstanceOf(ValidationException.class);

        assertThat(registeredClientRepository.findByClientId(registration.clientId())).isNull();
    }

    @Test
    void shouldNotPersistPublicClientWithInvalidRedirectUri() {
        PublicClientRegistration registration =
                new PublicClientRegistration(
                        "integration-invalid-redirect",
                        "Invalid Redirect Client",
                        Set.of("https://example.com/*"),
                        Set.of(),
                        Set.of(OidcScopes.OPENID));

        assertThatThrownBy(() -> registrationService.registerPublicClient(registration))
                .isInstanceOf(ValidationException.class);

        assertThat(registeredClientRepository.findByClientId(registration.clientId())).isNull();
    }

    @Test
    void shouldRegisterConfidentialUserClient() {
        String rawSecret = "integration-confidential-user-secret";

        ConfidentialUserClientRegistration registration =
                new ConfidentialUserClientRegistration(
                        "integration-cinema-bff",
                        "Integration Cinema BFF",
                        rawSecret,
                        Set.of("http://127.0.0.1:8080/login/oauth2/code/cinema"),
                        Set.of("http://127.0.0.1:8080"),
                        Set.of(OidcScopes.OPENID, OidcScopes.PROFILE, "booking:read"));

        RegisteredClientRegistrationResult result =
                registrationService.registerConfidentialUserClient(registration);

        RegisteredClient persisted =
                registeredClientRepository.findByClientId("integration-cinema-bff");

        assertThat(persisted).isNotNull();
        assertThat(result.id()).isEqualTo(persisted.getId());

        assertThat(persisted.getClientSecret()).isNotEqualTo(rawSecret);

        assertThat(passwordEncoder.matches(rawSecret, persisted.getClientSecret())).isTrue();

        assertThat(persisted.getClientAuthenticationMethods())
                .containsOnly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        assertThat(persisted.getAuthorizationGrantTypes())
                .containsOnly(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN);

        assertThat(persisted.getAuthorizationGrantTypes())
                .doesNotContain(AuthorizationGrantType.CLIENT_CREDENTIALS);

        assertThat(persisted.getClientSettings().isRequireProofKey()).isTrue();

        assertThat(persisted.getTokenSettings().getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(15));

        assertThat(persisted.getTokenSettings().getRefreshTokenTimeToLive())
                .isEqualTo(Duration.ofDays(30));

        assertThat(persisted.getTokenSettings().isReuseRefreshTokens()).isFalse();

        assertRegistrationAudit(
                "integration-cinema-bff",
                "clientType=CONFIDENTIAL_USER",
                rawSecret,
                persisted.getClientSecret());
    }

    @Test
    void shouldRejectDuplicateConfidentialUserClient() {
        ConfidentialUserClientRegistration registration =
                confidentialUserRegistration("duplicate-confidential-client");

        registrationService.registerConfidentialUserClient(registration);

        assertThatThrownBy(() -> registrationService.registerConfidentialUserClient(registration))
                .isInstanceOf(ConflictException.class);
    }

    private static ConfidentialUserClientRegistration confidentialUserRegistration(
            String clientId) {

        return new ConfidentialUserClientRegistration(
                clientId,
                "Test Confidential User Client",
                "test-confidential-user-secret",
                Set.of("http://127.0.0.1:8080/login/oauth2/code/cinema"),
                Set.of("http://127.0.0.1:8080"),
                Set.of(OidcScopes.OPENID, OidcScopes.PROFILE, "booking:read"));
    }

    private void assertRegistrationAudit(
            String clientId, String expectedMetadata, String rawSecret, String encodedSecret) {

        List<SecurityAuditEvent> events =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.OAUTH2_CLIENT, clientId);

        assertThat(events)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType())
                                    .isEqualTo(SecurityAuditEventType.OAUTH2_CLIENT_REGISTERED);

                            assertThat(event.getActorType())
                                    .isEqualTo(SecurityAuditActorType.SYSTEM);

                            assertThat(event.getActorReference()).isEqualTo(CommonConstants.SYSTEM);

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.OAUTH2_CLIENT);

                            assertThat(event.getTargetReference()).isEqualTo(clientId);

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getReason()).isNull();

                            assertThat(event.getMetadata()).isEqualTo(expectedMetadata);

                            assertThat(event.getOccurredAt()).isNotNull();

                            assertThat(event.getCreatedAt()).isNotNull();

                            assertThat(event.getUpdatedAt()).isNotNull();

                            if (rawSecret != null) {
                                assertThat(event.getTargetReference()).doesNotContain(rawSecret);

                                assertThat(event.getMetadata()).doesNotContain(rawSecret);
                            }

                            if (encodedSecret != null) {
                                assertThat(event.getTargetReference())
                                        .doesNotContain(encodedSecret);

                                assertThat(event.getMetadata()).doesNotContain(encodedSecret);
                            }
                        });
    }
}
