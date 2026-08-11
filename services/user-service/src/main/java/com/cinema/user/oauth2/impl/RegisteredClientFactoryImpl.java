package com.cinema.user.oauth2.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import com.cinema.common.core.id.UuidGenerator;
import com.cinema.common.exception.exception.ValidationException;
import com.cinema.user.exception.UserErrorCode;
import com.cinema.user.oauth2.RegisteredClientFactory;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

@Component
public class RegisteredClientFactoryImpl
        implements RegisteredClientFactory {

    private static final Pattern CLIENT_ID_PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$");

    private static final Pattern SCOPE_PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9:._-]{0,99}$");

    private static final Duration REFRESH_TOKEN_LIFETIME = Duration.ofDays(30);

    private static final Set<String> HUMAN_OIDC_SCOPES = Set.of(
            OidcScopes.OPENID,
            OidcScopes.PROFILE,
            OidcScopes.EMAIL,
            OidcScopes.ADDRESS,
            OidcScopes.PHONE);

    private static final Duration USER_ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);

    private static final Duration SERVICE_ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(5);

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    public RegisteredClientFactoryImpl(
            PasswordEncoder passwordEncoder,
            Clock clock) {

        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    public RegisteredClient createPublicClient(
            PublicClientRegistration registration) {

        if (registration == null) {
            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        String clientId = requireClientId(
                registration.clientId());

        String clientName = requireText(
                registration.clientName(),
                UserErrorCode.OAUTH2_CLIENT_NAME_REQUIRED);

        Set<String> redirectUris = validateRedirectUris(
                registration.redirectUris(),
                true);

        Set<String> postLogoutRedirectUris = validateRedirectUris(
                registration.postLogoutRedirectUris(),
                false);

        Set<String> scopes = normalizeScopes(
                registration.scopes());

        RegisteredClient.Builder builder = RegisteredClient
                .withId(UuidGenerator.next().toString())
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now(clock))
                .clientName(clientName)
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.NONE)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(true)
                                .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(USER_ACCESS_TOKEN_LIFETIME)
                        .build());

        addAll(builder::redirectUri, redirectUris);
        addAll(builder::postLogoutRedirectUri, postLogoutRedirectUris);
        addAll(builder::scope, scopes);

        return builder.build();
    }

    @Override
    public RegisteredClient createServiceClient(
            ServiceClientRegistration registration) {

        if (registration == null) {
            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        String clientId = requireClientId(
                registration.clientId());

        String clientName = requireText(
                registration.clientName(),
                UserErrorCode.OAUTH2_CLIENT_NAME_REQUIRED);

        String rawClientSecret = requireText(
                registration.rawClientSecret(),
                UserErrorCode.OAUTH2_CLIENT_SECRET_REQUIRED);

        Set<String> scopes = normalizeScopes(
                registration.scopes());

        if (scopes.stream()
                .anyMatch(HUMAN_OIDC_SCOPES::contains)) {

            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_SCOPE_INVALID);
        }

        RegisteredClient.Builder builder = RegisteredClient
                .withId(UuidGenerator.next().toString())
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now(clock))
                .clientSecret(
                        passwordEncoder.encode(
                                rawClientSecret))
                .clientName(clientName)
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        SERVICE_ACCESS_TOKEN_LIFETIME)
                                .build());

        addAll(builder::scope, scopes);

        return builder.build();
    }

    private static String requireClientId(
            String clientId) {

        String value = requireText(
                clientId,
                UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);

        if (!CLIENT_ID_PATTERN.matcher(value).matches()) {
            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        return value;
    }

    private static Set<String> normalizeScopes(
            Set<String> suppliedScopes) {

        if (suppliedScopes == null || suppliedScopes.isEmpty()) {
            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_SCOPE_INVALID);
        }

        Set<String> normalized = new LinkedHashSet<>();

        for (String scope : suppliedScopes) {
            if (scope == null) {
                throw validation(
                        UserErrorCode.OAUTH2_CLIENT_SCOPE_INVALID);
            }

            String value = scope.trim();

            if (!SCOPE_PATTERN.matcher(value).matches()) {
                throw validation(
                        UserErrorCode.OAUTH2_CLIENT_SCOPE_INVALID);
            }

            normalized.add(value);
        }

        return Set.copyOf(normalized);
    }

    private static Set<String> validateRedirectUris(
            Set<String> suppliedUris,
            boolean required) {

        if (suppliedUris == null || suppliedUris.isEmpty()) {
            if (required) {
                throw validation(
                        UserErrorCode.OAUTH2_CLIENT_REDIRECT_URI_INVALID);
            }

            return Set.of();
        }

        Set<String> validated = new LinkedHashSet<>();

        for (String value : suppliedUris) {
            validated.add(validateRedirectUri(value));
        }

        return Set.copyOf(validated);
    }

    private static String validateRedirectUri(
            String value) {

        if (value == null || value.isBlank() || value.contains("*")) {
            throw invalidRedirectUri();
        }

        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (!uri.isAbsolute()
                    || scheme == null
                    || host == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) {

                throw invalidRedirectUri();
            }

            String normalizedScheme = scheme.toLowerCase(
                    Locale.ROOT);

            if ("https".equals(normalizedScheme)) {
                return uri.toASCIIString();
            }

            if ("http".equals(normalizedScheme)
                    && isLoopbackHost(host)) {

                return uri.toASCIIString();
            }

            throw invalidRedirectUri();
        } catch (URISyntaxException exception) {
            throw invalidRedirectUri();
        }
    }

    private static boolean isLoopbackHost(
            String host) {

        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "::1".equals(host)
                || "[::1]".equals(host);
    }

    private static String requireText(
            String value,
            UserErrorCode errorCode) {

        if (value == null || value.isBlank()) {
            throw validation(errorCode);
        }

        return value.trim();
    }

    private static void addAll(
            Consumer<String> consumer,
            Set<String> values) {

        values.forEach(consumer);
    }

    private static ValidationException invalidRedirectUri() {
        return validation(
                UserErrorCode.OAUTH2_CLIENT_REDIRECT_URI_INVALID);
    }

    private static ValidationException validation(
            UserErrorCode errorCode) {

        return new ValidationException(errorCode);
    }

    @Override
    public RegisteredClient createConfidentialUserClient(
            ConfidentialUserClientRegistration registration) {

        if (registration == null) {
            throw validation(
                    UserErrorCode.OAUTH2_CLIENT_ID_REQUIRED);
        }

        String clientId = requireClientId(
                registration.clientId());

        String clientName = requireText(
                registration.clientName(),
                UserErrorCode.OAUTH2_CLIENT_NAME_REQUIRED);

        String rawClientSecret = requireText(
                registration.rawClientSecret(),
                UserErrorCode.OAUTH2_CLIENT_SECRET_REQUIRED);

        Set<String> redirectUris = validateRedirectUris(
                registration.redirectUris(),
                true);

        Set<String> postLogoutRedirectUris = validateRedirectUris(
                registration.postLogoutRedirectUris(),
                false);

        Set<String> scopes = normalizeScopes(
                registration.scopes());

        RegisteredClient.Builder builder = RegisteredClient
                .withId(UuidGenerator.next().toString())
                .clientId(clientId)
                .clientIdIssuedAt(Instant.now(clock))
                .clientSecret(
                        passwordEncoder.encode(
                                rawClientSecret))
                .clientName(clientName)
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN)
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(true)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(
                                        USER_ACCESS_TOKEN_LIFETIME)
                                .refreshTokenTimeToLive(
                                        REFRESH_TOKEN_LIFETIME)
                                .reuseRefreshTokens(false)
                                .build());

        addAll(builder::redirectUri, redirectUris);
        addAll(
                builder::postLogoutRedirectUri,
                postLogoutRedirectUris);
        addAll(builder::scope, scopes);

        return builder.build();
    }
}
