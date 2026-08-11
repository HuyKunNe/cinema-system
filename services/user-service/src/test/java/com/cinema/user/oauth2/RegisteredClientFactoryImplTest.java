package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.oauth2.impl.RegisteredClientFactoryImpl;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

@ExtendWith(MockitoExtension.class)
class RegisteredClientFactoryImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-10T03:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(
            FIXED_INSTANT,
            ZoneOffset.UTC);

    private static final String RAW_SECRET = "local-test-secret";

    private static final String CONFIDENTIAL_SECRET = "confidential-user-client-secret";

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegisteredClientFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new RegisteredClientFactoryImpl(
                passwordEncoder,
                FIXED_CLOCK);
    }

    @Test
    void createPublicClientShouldApplyPkcePolicy() {
        RegisteredClient client = factory.createPublicClient(
                publicRegistration());

        assertThat(client.getClientAuthenticationMethods())
                .containsExactly(
                        ClientAuthenticationMethod.NONE);

        assertThat(client.getClientSecret()).isNull();

        assertThat(client.getAuthorizationGrantTypes())
                .containsExactly(AuthorizationGrantType.AUTHORIZATION_CODE);

        assertThat(client.getClientSettings()
                .isRequireProofKey())
                .isTrue();

        assertThat(client.getClientSettings()
                .isRequireAuthorizationConsent())
                .isTrue();

        assertThat(client.getTokenSettings()
                .getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(15));

        assertThat(client.getId())
                .satisfies(id -> assertThat(
                        java.util.UUID.fromString(id).version())
                        .isEqualTo(7));

        assertThat(client.getClientIdIssuedAt())
                .isEqualTo(FIXED_INSTANT);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "ftp://example.com/callback",
            "https://user:password@example.com/callback",
            "https://example.com/callback#fragment",
            "https://example.com/callback?dynamic=true",
            "http://production.example.com/callback",
            "https://example.com/*"
    })
    void createPublicClientShouldRejectInvalidRedirectUri(
            String redirectUri) {

        PublicClientRegistration registration = new PublicClientRegistration(
                "cinema-web",
                "Cinema Web",
                Set.of(redirectUri),
                Set.of(),
                Set.of(OidcScopes.OPENID));

        assertThatThrownBy(() -> factory.createPublicClient(registration))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createPublicClientShouldAllowLoopbackHttpRedirectUri() {
        RegisteredClient client = factory.createPublicClient(
                publicRegistration());

        assertThat(client.getRedirectUris())
                .containsExactly(
                        "http://127.0.0.1:3000/callback");
    }

    @Test
    void createServiceClientShouldApplyServicePolicy() {
        when(passwordEncoder.encode(RAW_SECRET))
                .thenReturn("{bcrypt}encoded-secret");

        RegisteredClient client = factory.createServiceClient(
                serviceRegistration(
                        Set.of("inventory:write")));

        assertThat(client.getClientSecret())
                .isEqualTo("{bcrypt}encoded-secret");

        verify(passwordEncoder).encode(RAW_SECRET);

        assertThat(client.getClientAuthenticationMethods())
                .containsExactly(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        assertThat(client.getAuthorizationGrantTypes())
                .containsExactly(
                        AuthorizationGrantType.CLIENT_CREDENTIALS);

        assertThat(client.getRedirectUris()).isEmpty();

        assertThat(client.getTokenSettings()
                .getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(5));

        assertThat(client.getClientIdIssuedAt())
                .isEqualTo(FIXED_INSTANT);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    void createServiceClientShouldRejectBlankSecret(
            String rawSecret) {

        ServiceClientRegistration registration = new ServiceClientRegistration(
                "inventory-service",
                "Inventory Service",
                rawSecret,
                Set.of("inventory:write"));

        assertThatThrownBy(() -> factory.createServiceClient(registration))
                .isInstanceOf(ValidationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            OidcScopes.OPENID,
            OidcScopes.PROFILE,
            OidcScopes.EMAIL,
            OidcScopes.ADDRESS,
            OidcScopes.PHONE
    })
    void createServiceClientShouldRejectHumanOidcScope(
            String scope) {

        assertThatThrownBy(() -> factory.createServiceClient(
                serviceRegistration(
                        Set.of(scope))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void serviceRegistrationToStringShouldRedactSecret() {
        ServiceClientRegistration registration = serviceRegistration(
                Set.of("inventory:write"));

        assertThat(registration.toString())
                .contains("[REDACTED]")
                .doesNotContain(RAW_SECRET);
    }

    @Test
    void shouldCreateConfidentialUserClient() {
        String encodedSecret = "{bcrypt}encoded-confidential-user-secret";

        when(passwordEncoder.encode(
                CONFIDENTIAL_SECRET))
                .thenReturn(encodedSecret);

        ConfidentialUserClientRegistration registration = new ConfidentialUserClientRegistration(
                "cinema-bff",
                "Cinema BFF",
                CONFIDENTIAL_SECRET,
                Set.of(
                        "http://127.0.0.1:8080/login/oauth2/code/cinema"),
                Set.of(
                        "http://127.0.0.1:8080"),
                Set.of(
                        OidcScopes.OPENID,
                        OidcScopes.PROFILE,
                        "booking:read",
                        "booking:create"));

        RegisteredClient client = factory
                .createConfidentialUserClient(
                        registration);

        assertThat(client.getClientAuthenticationMethods())
                .containsOnly(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        assertThat(client.getAuthorizationGrantTypes())
                .containsOnly(
                        AuthorizationGrantType.AUTHORIZATION_CODE,
                        AuthorizationGrantType.REFRESH_TOKEN);

        assertThat(client.getAuthorizationGrantTypes())
                .doesNotContain(
                        AuthorizationGrantType.CLIENT_CREDENTIALS);

        assertThat(client.getClientSettings()
                .isRequireProofKey())
                .isTrue();

        assertThat(client.getClientSettings()
                .isRequireAuthorizationConsent())
                .isTrue();

        assertThat(client.getTokenSettings()
                .getAccessTokenTimeToLive())
                .isEqualTo(Duration.ofMinutes(15));

        assertThat(client.getTokenSettings()
                .getRefreshTokenTimeToLive())
                .isEqualTo(Duration.ofDays(30));

        assertThat(client.getTokenSettings()
                .isReuseRefreshTokens())
                .isFalse();

        assertThat(client.getClientSecret())
                .isEqualTo(encodedSecret);

        verify(passwordEncoder)
                .encode(CONFIDENTIAL_SECRET);
    }

    @Test
    void confidentialUserRegistrationToStringShouldRedactSecret() {
        ConfidentialUserClientRegistration registration = new ConfidentialUserClientRegistration(
                "cinema-bff",
                "Cinema BFF",
                CONFIDENTIAL_SECRET,
                Set.of(
                        "http://127.0.0.1:8080/callback"),
                Set.of(),
                Set.of("booking:read"));

        assertThat(registration.toString())
                .contains("[REDACTED]")
                .doesNotContain(CONFIDENTIAL_SECRET);
    }

    private static PublicClientRegistration publicRegistration() {
        return new PublicClientRegistration(
                "cinema-web",
                "Cinema Web",
                Set.of(
                        "http://127.0.0.1:3000/callback"),
                Set.of(
                        "http://127.0.0.1:3000"),
                Set.of(
                        OidcScopes.OPENID,
                        OidcScopes.PROFILE,
                        "booking:read",
                        "booking:create"));
    }

    private static ServiceClientRegistration serviceRegistration(
            Set<String> scopes) {

        return new ServiceClientRegistration(
                "inventory-service",
                "Inventory Service",
                RAW_SECRET,
                scopes);
    }
}
