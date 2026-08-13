package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082"
})
@AutoConfigureMockMvc
@Transactional
class TokenRevocationEndpointIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String CLIENT_ID =
            "r25-11-7-revocation-client";

    private static final String RAW_CLIENT_SECRET =
            "r25-11-7-revocation-client-secret";

    private static final String PRINCIPAL_NAME =
            "revocation-user@example.com";

    private static final String REDIRECT_URI =
            "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String SCOPE =
            "booking:read";

    private static final Instant ISSUED_AT =
            Instant.parse("2026-08-12T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Test
    void shouldRevokeRefreshTokenForAuthenticatedClient()
            throws Exception {

        RegisteredClient registeredClient =
                saveRegisteredClient();

        OAuth2Authorization authorization =
                saveAuthorization(
                        registeredClient,
                        "revocation-authorization-001",
                        "revocation-access-token-001",
                        "revocation-refresh-token-001");

        assertActiveHistory(
                authorization.getId());

        mockMvc.perform(
                post("/oauth2/revoke")
                        .with(httpBasic(
                                CLIENT_ID,
                                RAW_CLIENT_SECRET))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "token",
                                authorization
                                        .getRefreshToken()
                                        .getToken()
                                        .getTokenValue())
                        .param(
                                "token_type_hint",
                                "refresh_token"))
                .andExpect(status().isOk());

        OAuth2Authorization persistedAuthorization =
                authorizationService.findById(
                        authorization.getId());

        assertThat(persistedAuthorization)
                .isNotNull();

        assertThat(persistedAuthorization.getRefreshToken())
                .isNotNull();

        assertThat(persistedAuthorization
                .getRefreshToken()
                .isInvalidated())
                .isTrue();

        assertRevokedHistory(
                authorization.getId());
    }

    @Test
    void shouldRejectRevokedRefreshTokenAtTokenEndpoint()
            throws Exception {

        RegisteredClient registeredClient =
                saveRegisteredClient();

        OAuth2Authorization authorization =
                saveAuthorization(
                        registeredClient,
                        "revocation-authorization-002",
                        "revocation-access-token-002",
                        "revocation-refresh-token-002");

        String refreshToken = authorization
                .getRefreshToken()
                .getToken()
                .getTokenValue();

        mockMvc.perform(
                post("/oauth2/revoke")
                        .with(httpBasic(
                                CLIENT_ID,
                                RAW_CLIENT_SECRET))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "token",
                                refreshToken)
                        .param(
                                "token_type_hint",
                                "refresh_token"))
                .andExpect(status().isOk());

        mockMvc.perform(
                post("/oauth2/token")
                        .with(httpBasic(
                                CLIENT_ID,
                                RAW_CLIENT_SECRET))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "grant_type",
                                "refresh_token")
                        .param(
                                "refresh_token",
                                refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("invalid_grant"));

        assertRevokedHistory(
                authorization.getId());
    }

    @Test
    void shouldReturnSuccessForUnknownTokenWithoutRevealingState()
            throws Exception {

        saveRegisteredClient();

        mockMvc.perform(
                post("/oauth2/revoke")
                        .with(httpBasic(
                                CLIENT_ID,
                                RAW_CLIENT_SECRET))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "token",
                                "unknown-refresh-token")
                        .param(
                                "token_type_hint",
                                "refresh_token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectRevocationWithoutClientAuthentication()
            throws Exception {

        RegisteredClient registeredClient =
                saveRegisteredClient();

        OAuth2Authorization authorization =
                saveAuthorization(
                        registeredClient,
                        "revocation-authorization-003",
                        "revocation-access-token-003",
                        "revocation-refresh-token-003");

        mockMvc.perform(
                post("/oauth2/revoke")
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "token",
                                authorization
                                        .getRefreshToken()
                                        .getToken()
                                        .getTokenValue())
                        .param(
                                "token_type_hint",
                                "refresh_token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("invalid_client"));

        OAuth2Authorization persistedAuthorization =
                authorizationService.findById(
                        authorization.getId());

        assertThat(persistedAuthorization)
                .isNotNull();

        assertThat(persistedAuthorization
                .getRefreshToken()
                .isInvalidated())
                .isFalse();

        assertActiveHistory(
                authorization.getId());
    }

    @Test
    void shouldRejectRevocationWithIncorrectClientSecret()
            throws Exception {

        RegisteredClient registeredClient =
                saveRegisteredClient();

        OAuth2Authorization authorization =
                saveAuthorization(
                        registeredClient,
                        "revocation-authorization-004",
                        "revocation-access-token-004",
                        "revocation-refresh-token-004");

        mockMvc.perform(
                post("/oauth2/revoke")
                        .with(httpBasic(
                                CLIENT_ID,
                                RAW_CLIENT_SECRET
                                        + "-incorrect"))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "token",
                                authorization
                                        .getRefreshToken()
                                        .getToken()
                                        .getTokenValue())
                        .param(
                                "token_type_hint",
                                "refresh_token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("invalid_client"));

        OAuth2Authorization persistedAuthorization =
                authorizationService.findById(
                        authorization.getId());

        assertThat(persistedAuthorization)
                .isNotNull();

        assertThat(persistedAuthorization
                .getRefreshToken()
                .isInvalidated())
                .isFalse();

        assertActiveHistory(
                authorization.getId());
    }

    private RegisteredClient saveRegisteredClient() {
        RegisteredClient registeredClient =
                RegisteredClient
                        .withId(
                                UUID.randomUUID()
                                        .toString())
                        .clientId(
                                CLIENT_ID)
                        .clientIdIssuedAt(
                                ISSUED_AT)
                        .clientSecret(
                                passwordEncoder.encode(
                                        RAW_CLIENT_SECRET))
                        .clientName(
                                "R25.11.7 Revocation Client")
                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod
                                        .CLIENT_SECRET_BASIC)
                        .authorizationGrantType(
                                AuthorizationGrantType
                                        .AUTHORIZATION_CODE)
                        .authorizationGrantType(
                                AuthorizationGrantType
                                        .REFRESH_TOKEN)
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
                registeredClient);

        return registeredClient;
    }

    private OAuth2Authorization saveAuthorization(
            RegisteredClient registeredClient,
            String authorizationId,
            String accessTokenValue,
            String refreshTokenValue) {

        OAuth2AccessToken accessToken =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        accessTokenValue,
                        ISSUED_AT,
                        ISSUED_AT.plus(
                                Duration.ofMinutes(
                                        15)),
                        Set.of(
                                SCOPE));

        OAuth2RefreshToken refreshToken =
                new OAuth2RefreshToken(
                        refreshTokenValue,
                        ISSUED_AT,
                        ISSUED_AT.plus(
                                Duration.ofDays(
                                        30)));

        OAuth2Authorization authorization =
                OAuth2Authorization
                        .withRegisteredClient(
                                registeredClient)
                        .id(
                                authorizationId)
                        .principalName(
                                PRINCIPAL_NAME)
                        .authorizationGrantType(
                                AuthorizationGrantType
                                        .AUTHORIZATION_CODE)
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

    private void assertActiveHistory(
            String authorizationId) {

        List<RefreshTokenHistory> histories =
                refreshTokenHistoryRepository
                        .findAllByAuthorizationId(
                                authorizationId);

        assertThat(histories)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getStatus())
                            .isEqualTo(
                                    RefreshTokenStatus.ACTIVE);

                    assertThat(history.getRevokedAt())
                            .isNull();
                });
    }

    private void assertRevokedHistory(
            String authorizationId) {

        List<RefreshTokenHistory> histories =
                refreshTokenHistoryRepository
                        .findAllByAuthorizationId(
                                authorizationId);

        assertThat(histories)
                .singleElement()
                .satisfies(history -> {
                    assertThat(history.getStatus())
                            .isEqualTo(
                                    RefreshTokenStatus.REVOKED);

                    assertThat(history.getRevokedAt())
                            .isNotNull();
                });
    }
}
