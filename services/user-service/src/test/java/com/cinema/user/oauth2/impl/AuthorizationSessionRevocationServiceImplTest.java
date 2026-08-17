package com.cinema.user.oauth2.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import com.cinema.user.oauth2.AuthorizationSessionRevocationService;
import com.cinema.user.oauth2.OAuth2AuthorizationQueryRepository;
import com.cinema.user.oauth2.audit.RevocationAuditRecorder;
import com.cinema.user.oauth2.audit.RevocationAuditTargetType;
import com.cinema.user.oauth2.audit.RevocationReason;

@ExtendWith(MockitoExtension.class)
class AuthorizationSessionRevocationServiceImplTest {

    private static final String AUTHORIZATION_ID = "authorization-001";

    private static final String SECOND_AUTHORIZATION_ID = "authorization-002";

    private static final String REGISTERED_CLIENT_ID = "registered-client-001";

    private static final String CLIENT_ID = "r25-11-8-revocation-client";

    private static final String PRINCIPAL_NAME = "revocation-user@example.com";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE = "booking:read";

    private static final Instant ISSUED_AT = Instant.parse("2026-08-13T08:00:00Z");

    @Mock
    private OAuth2AuthorizationQueryRepository queryRepository;

    @Mock
    private OAuth2AuthorizationService authorizationService;

    @Mock
    private RevocationAuditRecorder auditRecorder;

    private AuthorizationSessionRevocationService revocationService;

    @BeforeEach
    void setUp() {
        revocationService = new AuthorizationSessionRevocationServiceImpl(
                queryRepository,
                authorizationService,
                auditRecorder);
    }

    @Test
    void shouldRevokeAuthorizationTokensByPrincipalNameAndRecordAudit() {
        OAuth2Authorization authorization = createAuthorization(AUTHORIZATION_ID);

        when(queryRepository.findIdsByPrincipalName(
                PRINCIPAL_NAME))
                .thenReturn(List.of(AUTHORIZATION_ID));

        when(authorizationService.findById(
                AUTHORIZATION_ID))
                .thenReturn(authorization);

        revocationService.revokeByPrincipalName(
                PRINCIPAL_NAME,
                RevocationReason.ACCOUNT_LOCKED);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(OAuth2Authorization.class);

        verify(authorizationService)
                .save(captor.capture());

        assertInvalidated(captor.getValue());

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.USER,
                        PRINCIPAL_NAME,
                        RevocationReason.ACCOUNT_LOCKED,
                        1);
    }

    @Test
    void shouldRevokeAuthorizationTokensByRegisteredClientIdAndRecordAudit() {
        OAuth2Authorization authorization = createAuthorization(AUTHORIZATION_ID);

        when(queryRepository.findIdsByRegisteredClientId(
                REGISTERED_CLIENT_ID))
                .thenReturn(List.of(AUTHORIZATION_ID));

        when(authorizationService.findById(
                AUTHORIZATION_ID))
                .thenReturn(authorization);

        revocationService.revokeByRegisteredClientId(
                REGISTERED_CLIENT_ID,
                CLIENT_ID,
                RevocationReason.CLIENT_DEACTIVATED);

        ArgumentCaptor<OAuth2Authorization> captor = ArgumentCaptor.forClass(
                OAuth2Authorization.class);

        verify(authorizationService)
                .save(captor.capture());

        assertInvalidated(captor.getValue());

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.CLIENT,
                        CLIENT_ID,
                        RevocationReason.CLIENT_DEACTIVATED,
                        1);
    }

    @Test
    void shouldCountOnlyAuthorizationsChangedByRevocation() {
        OAuth2Authorization activeAuthorization = createAuthorization(AUTHORIZATION_ID);

        OAuth2Authorization revokedAuthorization = invalidateAll(
                createAuthorization(SECOND_AUTHORIZATION_ID));

        when(queryRepository.findIdsByPrincipalName(
                PRINCIPAL_NAME))
                .thenReturn(
                        List.of(
                                AUTHORIZATION_ID,
                                SECOND_AUTHORIZATION_ID));

        when(authorizationService.findById(
                AUTHORIZATION_ID))
                .thenReturn(activeAuthorization);

        when(authorizationService.findById(
                SECOND_AUTHORIZATION_ID))
                .thenReturn(revokedAuthorization);

        revocationService.revokeByPrincipalName(
                PRINCIPAL_NAME,
                RevocationReason.PASSWORD_CHANGED);

        verify(authorizationService)
                .save(org.mockito.ArgumentMatchers.any(OAuth2Authorization.class));

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.USER,
                        PRINCIPAL_NAME,
                        RevocationReason.PASSWORD_CHANGED,
                        1);
    }

    @Test
    void shouldIgnoreAuthorizationRemovedDuringRevocationAndRecordZeroCount() {
        when(queryRepository.findIdsByPrincipalName(
                PRINCIPAL_NAME))
                .thenReturn(List.of(AUTHORIZATION_ID));

        when(authorizationService.findById(
                AUTHORIZATION_ID))
                .thenReturn(null);

        revocationService.revokeByPrincipalName(
                PRINCIPAL_NAME,
                RevocationReason.ACCOUNT_DISABLED);

        verify(authorizationService, never())
                .save(org.mockito.ArgumentMatchers.any());

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.USER,
                        PRINCIPAL_NAME,
                        RevocationReason.ACCOUNT_DISABLED,
                        0);
    }

    @Test
    void shouldNotSaveAlreadyRevokedAuthorizationAndRecordZeroCount() {
        OAuth2Authorization revokedAuthorization = invalidateAll(
                createAuthorization(AUTHORIZATION_ID));

        when(queryRepository.findIdsByPrincipalName(
                PRINCIPAL_NAME))
                .thenReturn(
                        List.of(AUTHORIZATION_ID));

        when(authorizationService.findById(
                AUTHORIZATION_ID))
                .thenReturn(revokedAuthorization);

        revocationService.revokeByPrincipalName(
                PRINCIPAL_NAME,
                RevocationReason.PASSWORD_RESET);

        verify(authorizationService, never())
                .save(org.mockito.ArgumentMatchers.any());

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.USER,
                        PRINCIPAL_NAME,
                        RevocationReason.PASSWORD_RESET,
                        0);
    }

    @Test
    void shouldRecordAuditWhenNoAuthorizationMatches() {
        when(queryRepository.findIdsByPrincipalName(PRINCIPAL_NAME))
                .thenReturn(List.of());

        revocationService.revokeByPrincipalName(
                PRINCIPAL_NAME,
                RevocationReason.ADMIN_USER_REVOCATION);

        verify(authorizationService, never())
                .findById(org.mockito.ArgumentMatchers.anyString());

        verify(authorizationService, never())
                .save(org.mockito.ArgumentMatchers.any());

        verify(auditRecorder)
                .record(
                        RevocationAuditTargetType.USER,
                        PRINCIPAL_NAME,
                        RevocationReason.ADMIN_USER_REVOCATION,
                        0);
    }

    private OAuth2Authorization createAuthorization(String authorizationId) {

        RegisteredClient registeredClient = RegisteredClient
                .withId(REGISTERED_CLIENT_ID)
                .clientId(CLIENT_ID)
                .clientSecret("{bcrypt}encoded-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT_URI)
                .scope(SCOPE)
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-" + authorizationId,
                ISSUED_AT,
                ISSUED_AT.plus(Duration.ofMinutes(15)),
                Set.of(SCOPE));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                "refresh-token-" + authorizationId,
                ISSUED_AT,
                ISSUED_AT.plus(Duration.ofDays(30)));

        OidcIdToken idToken = new OidcIdToken(
                "id-token-" + authorizationId,
                ISSUED_AT,
                ISSUED_AT.plus(Duration.ofMinutes(15)),
                Map.of(
                        "sub",
                        PRINCIPAL_NAME,
                        "aud",
                        List.of(CLIENT_ID)));

        return OAuth2Authorization
                .withRegisteredClient(registeredClient)
                .id(authorizationId)
                .principalName(PRINCIPAL_NAME)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizedScopes(Set.of(SCOPE))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .token(idToken)
                .build();
    }

    private OAuth2Authorization invalidateAll(
            OAuth2Authorization authorization) {

        OAuth2Authorization.Builder builder = OAuth2Authorization.from(authorization);

        builder.invalidate(
                authorization
                        .getAccessToken()
                        .getToken());

        builder.invalidate(
                authorization
                        .getRefreshToken()
                        .getToken());

        builder.invalidate(
                authorization
                        .getToken(OidcIdToken.class)
                        .getToken());

        return builder.build();
    }

    private void assertInvalidated(
            OAuth2Authorization authorization) {

        assertThat(authorization
                .getAccessToken()
                .isInvalidated())
                .isTrue();

        assertThat(authorization
                .getRefreshToken()
                .isInvalidated())
                .isTrue();

        assertThat(authorization
                .getToken(
                        OidcIdToken.class))
                .isNotNull();

        assertThat(authorization
                .getToken(
                        OidcIdToken.class)
                .isInvalidated())
                .isTrue();
    }
}
