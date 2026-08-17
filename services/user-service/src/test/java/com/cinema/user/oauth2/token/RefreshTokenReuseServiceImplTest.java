package com.cinema.user.oauth2.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.repository.RefreshTokenHistoryRepository;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class RefreshTokenReuseServiceImplTest {

    private static final String RAW_ROTATED_TOKEN = "rotated-refresh-token-value";

    private static final String ROTATED_TOKEN_HASH = "a".repeat(64);

    private static final String AUTHORIZATION_ID = "authorization-family-001";

    private static final String REGISTERED_CLIENT_ID = "registered-client-internal-001";

    private static final String CLIENT_ID = "cinema-bff";

    private static final String PRINCIPAL_NAME = "customer@example.com";

    private static final String CURRENT_ACCESS_TOKEN = "current-access-token-value";

    private static final String CURRENT_REFRESH_TOKEN = "current-refresh-token-value";

    private static final Instant NOW = Instant.parse("2026-08-12T04:30:00Z");

    private static final OffsetDateTime NOW_OFFSET = OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC);

    @Mock private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Mock private RefreshTokenHasher refreshTokenHasher;

    @Mock private OAuth2AuthorizationService authorizationService;
    @Mock private SecurityAuditRecorder securityAuditRecorder;
    private RefreshTokenReuseServiceImpl refreshTokenReuseService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

        refreshTokenReuseService =
                new RefreshTokenReuseServiceImpl(
                        refreshTokenHistoryRepository,
                        refreshTokenHasher,
                        securityAuditRecorder,
                        clock);
    }

    @Test
    void shouldIgnoreUnknownTokenHash() {
        when(refreshTokenHasher.hash(RAW_ROTATED_TOKEN)).thenReturn(ROTATED_TOKEN_HASH);

        when(refreshTokenHistoryRepository.findByTokenHashForUpdate(ROTATED_TOKEN_HASH))
                .thenReturn(Optional.empty());

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isFalse();

        verify(refreshTokenHasher).hash(RAW_ROTATED_TOKEN);

        verify(refreshTokenHistoryRepository).findByTokenHashForUpdate(ROTATED_TOKEN_HASH);

        verifyNoInteractions(authorizationService);

        verify(refreshTokenHistoryRepository, never())
                .revokeActiveTokensByAuthorizationId(
                        eq(AUTHORIZATION_ID),
                        eq(RefreshTokenStatus.ACTIVE),
                        eq(RefreshTokenStatus.REVOKED),
                        eq(NOW_OFFSET));

        verifyNoInteractions(securityAuditRecorder);
    }

    @Test
    void shouldIgnoreActiveRefreshToken() {
        RefreshTokenHistory activeHistory = mock(RefreshTokenHistory.class);

        when(activeHistory.isRotated()).thenReturn(false);

        when(refreshTokenHasher.hash(RAW_ROTATED_TOKEN)).thenReturn(ROTATED_TOKEN_HASH);

        when(refreshTokenHistoryRepository.findByTokenHashForUpdate(ROTATED_TOKEN_HASH))
                .thenReturn(Optional.of(activeHistory));

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isFalse();

        verify(activeHistory).isRotated();

        verify(activeHistory, never()).markReused(NOW_OFFSET);

        verify(refreshTokenHistoryRepository, never()).saveAndFlush(activeHistory);

        verifyNoInteractions(authorizationService);

        verifyNoInteractions(securityAuditRecorder);
    }

    @Test
    void shouldMarkRotatedTokenAsReused() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        OAuth2Authorization authorization = currentAuthorization();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(authorization);

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isTrue();

        verify(rotatedHistory).markReused(NOW_OFFSET);

        verify(refreshTokenHistoryRepository).saveAndFlush(rotatedHistory);
    }

    @Test
    void shouldRevokeActiveTokenFamily() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(null);

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isTrue();

        verify(refreshTokenHistoryRepository)
                .revokeActiveTokensByAuthorizationId(
                        AUTHORIZATION_ID,
                        RefreshTokenStatus.ACTIVE,
                        RefreshTokenStatus.REVOKED,
                        NOW_OFFSET);

        verify(authorizationService, never())
                .save(org.mockito.ArgumentMatchers.any(OAuth2Authorization.class));
    }

    @Test
    void shouldInvalidateCurrentAuthorizationTokens() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        OAuth2Authorization currentAuthorization = currentAuthorization();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(currentAuthorization);

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isTrue();

        ArgumentCaptor<OAuth2Authorization> captor =
                ArgumentCaptor.forClass(OAuth2Authorization.class);

        verify(authorizationService).save(captor.capture());

        OAuth2Authorization invalidated = captor.getValue();

        assertThat(invalidated.getId()).isEqualTo(AUTHORIZATION_ID);

        assertThat(invalidated.getAccessToken()).isNotNull();

        assertThat(invalidated.getAccessToken().isInvalidated()).isTrue();

        assertThat(invalidated.getRefreshToken()).isNotNull();

        assertThat(invalidated.getRefreshToken().isInvalidated()).isTrue();

        assertThat(invalidated.getAccessToken().getToken().getTokenValue())
                .isEqualTo(CURRENT_ACCESS_TOKEN);

        assertThat(invalidated.getRefreshToken().getToken().getTokenValue())
                .isEqualTo(CURRENT_REFRESH_TOKEN);
    }

    @Test
    void shouldUseOnlyTokenHashForHistoryLookup() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(null);

        refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        verify(refreshTokenHasher).hash(RAW_ROTATED_TOKEN);

        verify(refreshTokenHistoryRepository).findByTokenHashForUpdate(ROTATED_TOKEN_HASH);

        verify(refreshTokenHistoryRepository, never()).findByTokenHashForUpdate(RAW_ROTATED_TOKEN);
    }

    private void stubRotatedHistory(RefreshTokenHistory rotatedHistory) {

        when(refreshTokenHasher.hash(RAW_ROTATED_TOKEN)).thenReturn(ROTATED_TOKEN_HASH);

        when(refreshTokenHistoryRepository.findByTokenHashForUpdate(ROTATED_TOKEN_HASH))
                .thenReturn(Optional.of(rotatedHistory));

        when(rotatedHistory.isRotated()).thenReturn(true);

        when(rotatedHistory.getAuthorizationId()).thenReturn(AUTHORIZATION_ID);
    }

    private RefreshTokenHistory rotatedHistory() {
        return mock(RefreshTokenHistory.class);
    }

    private OAuth2Authorization currentAuthorization() {
        RegisteredClient registeredClient =
                RegisteredClient.withId(REGISTERED_CLIENT_ID)
                        .clientId(CLIENT_ID)
                        .clientSecret("{bcrypt}encoded-secret")
                        .clientName("Cinema BFF")
                        .clientAuthenticationMethod(
                                org.springframework.security.oauth2.core.ClientAuthenticationMethod
                                        .CLIENT_SECRET_BASIC)
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                        .redirectUri("http://127.0.0.1:8080" + "/login/oauth2/code/cinema")
                        .scope("booking:read")
                        .build();

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        CURRENT_ACCESS_TOKEN,
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(840),
                        Set.of("booking:read"));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        CURRENT_REFRESH_TOKEN,
                        NOW.minusSeconds(60),
                        NOW.plusSeconds(30L * 24 * 60 * 60));

        return OAuth2Authorization.withRegisteredClient(registeredClient)
                .id(AUTHORIZATION_ID)
                .principalName(PRINCIPAL_NAME)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of("booking:read"))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Test
    void shouldRecordSecurityAuditWhenReuseIsDetected() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(null);

        boolean detected =
                refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        assertThat(detected).isTrue();

        verify(securityAuditRecorder)
                .record(
                        new SecurityAuditRecord(
                                SecurityAuditEventType.REFRESH_TOKEN_REUSE_DETECTED,
                                SecurityAuditTargetType.AUTHORIZATION_SESSION,
                                AUTHORIZATION_ID,
                                SecurityAuditOutcome.SUCCESS,
                                "ROTATED_REFRESH_TOKEN_REUSED",
                                null));
    }

    @Test
    void securityAuditShouldNotContainRefreshTokenOrTokenHash() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(null);

        refreshTokenReuseService.detectAndRevoke(RAW_ROTATED_TOKEN, authorizationService);

        ArgumentCaptor<SecurityAuditRecord> auditCaptor =
                ArgumentCaptor.forClass(SecurityAuditRecord.class);

        verify(securityAuditRecorder).record(auditCaptor.capture());

        SecurityAuditRecord record = auditCaptor.getValue();

        assertThat(record.targetReference()).isEqualTo(AUTHORIZATION_ID);

        assertThat(record.targetReference()).doesNotContain(RAW_ROTATED_TOKEN, ROTATED_TOKEN_HASH);

        assertThat(record.reason()).doesNotContain(RAW_ROTATED_TOKEN, ROTATED_TOKEN_HASH);

        assertThat(record.metadata()).isNull();
    }

    @Test
    void auditFailureShouldPropagateAfterReuseDetection() {
        RefreshTokenHistory rotatedHistory = rotatedHistory();

        stubRotatedHistory(rotatedHistory);

        when(authorizationService.findById(AUTHORIZATION_ID)).thenReturn(null);

        doThrow(new IllegalStateException("simulated security audit failure"))
                .when(securityAuditRecorder)
                .record(any(SecurityAuditRecord.class));

        assertThatThrownBy(
                        () ->
                                refreshTokenReuseService.detectAndRevoke(
                                        RAW_ROTATED_TOKEN, authorizationService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("simulated security audit failure");

        verify(rotatedHistory).markReused(NOW_OFFSET);

        verify(refreshTokenHistoryRepository).saveAndFlush(rotatedHistory);

        verify(refreshTokenHistoryRepository)
                .revokeActiveTokensByAuthorizationId(
                        AUTHORIZATION_ID,
                        RefreshTokenStatus.ACTIVE,
                        RefreshTokenStatus.REVOKED,
                        NOW_OFFSET);
    }
}
