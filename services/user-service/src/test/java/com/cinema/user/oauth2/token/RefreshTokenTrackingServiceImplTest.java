package com.cinema.user.oauth2.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class RefreshTokenTrackingServiceImplTest {

    private static final String PREVIOUS_REFRESH_TOKEN = "previous-refresh-token";

    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token";

    private static final String PREVIOUS_TOKEN_HASH = "a".repeat(64);

    private static final Instant NOW = Instant.parse("2026-08-17T10:00:00Z");

    private static final Clock FIXED_CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Mock private RefreshTokenHasher refreshTokenHasher;

    @Mock private RefreshTokenHistory previousHistory;

    private RefreshTokenTrackingServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new RefreshTokenTrackingServiceImpl(
                        refreshTokenHistoryRepository, refreshTokenHasher, FIXED_CLOCK);
    }

    @Test
    void concurrentRotationShouldRejectNonActiveLockedHistory() {
        OAuth2Authorization previous = authorization(PREVIOUS_REFRESH_TOKEN, NOW.minusSeconds(60));

        OAuth2Authorization current = authorization(CURRENT_REFRESH_TOKEN, NOW);

        when(refreshTokenHasher.hash(PREVIOUS_REFRESH_TOKEN)).thenReturn(PREVIOUS_TOKEN_HASH);

        when(refreshTokenHistoryRepository.findByTokenHashForUpdate(PREVIOUS_TOKEN_HASH))
                .thenReturn(Optional.of(previousHistory));

        when(previousHistory.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.synchronize(previous, current))
                .isInstanceOfSatisfying(
                        OAuth2AuthenticationException.class,
                        exception ->
                                assertThat(exception.getError().getErrorCode())
                                        .isEqualTo(OAuth2ErrorCodes.INVALID_GRANT));

        verify(previousHistory).isActive();

        verify(previousHistory, never()).markRotated(any());

        verify(refreshTokenHistoryRepository, never()).save(any(RefreshTokenHistory.class));
    }

    private static OAuth2Authorization authorization(String refreshTokenValue, Instant issuedAt) {

        RegisteredClient client =
                RegisteredClient.withId("registered-client-id")
                        .clientId("cinema-bff")
                        .clientSecret("{noop}test-secret")
                        .clientName("Cinema BFF")
                        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://127.0.0.1:8080" + "/login/oauth2/code/cinema")
                        .scope("booking:read")
                        .build();

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        refreshTokenValue, issuedAt, issuedAt.plusSeconds(30L * 24 * 60 * 60));

        return OAuth2Authorization.withRegisteredClient(client)
                .id("authorization-id")
                .principalName("customer@example.com")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("booking:read"))
                .refreshToken(refreshToken)
                .build();
    }
}
