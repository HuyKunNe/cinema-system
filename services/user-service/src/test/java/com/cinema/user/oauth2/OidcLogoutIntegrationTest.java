package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.RefreshTokenHistory;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.token.RefreshTokenHasher;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.service.UserCredentialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@SpringBootTest(
        properties = {
            "spring.main.web-application-type=servlet",
            "cinema.user.authorization-server.issuer=http://localhost:8082",
            "cinema.user.authorization-server.jwt.audiences=cinema-api"
        })
@AutoConfigureMockMvc
class OidcLogoutIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String CLIENT_ID = "r25-11-7-oidc-logout-client";

    private static final String RAW_CLIENT_SECRET = "r25-11-7-oidc-logout-client-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String POST_LOGOUT_REDIRECT_URI = "http://127.0.0.1:8080";

    private static final String RAW_PASSWORD = "correct-password-123";

    private static final String KEY_ID = "cinema-oidc-logout-test-key";

    private static final String CODE_VERIFIER =
            "cinema-oidc-logout-pkce-verifier-123456789012345678901";

    private static final String NONCE = "cinema-oidc-logout-nonce";

    private static final String AUTHORIZATION_STATE = "oidc-logout-authorization-state";

    private static final String LOGOUT_STATE = "oidc-logout-state";

    private static final OffsetDateTime VERIFIED_AT = OffsetDateTime.parse("2026-08-12T08:00:00Z");

    private static final TestRsaKeyMaterial KEY_MATERIAL = TestRsaKeyMaterial.generate();

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Autowired private UserCredentialService userCredentialService;

    @Autowired private RoleRepository roleRepository;

    @Autowired private UserRoleRepository userRoleRepository;

    @Autowired private RegisteredClientFactory registeredClientFactory;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired private RefreshTokenHasher refreshTokenHasher;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureSigningProperties(DynamicPropertyRegistry registry) {

        registry.add("cinema.user.authorization-server.signing.enabled", () -> "true");

        registry.add("cinema.user.authorization-server.signing.key-id", () -> KEY_ID);

        registry.add(
                "cinema.user.authorization-server.signing.private-key-location",
                KEY_MATERIAL::privateKeyLocation);

        registry.add(
                "cinema.user.authorization-server.signing.public-key-location",
                KEY_MATERIAL::publicKeyLocation);
    }

    @Test
    void shouldLogoutWithValidIdTokenHint() throws Exception {

        User user = createActiveUser();

        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String authorizationCode = authorize(session, createCodeChallenge(CODE_VERIFIER));

        JsonNode tokenResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String idToken = tokenResponse.required("id_token").asText();

        String refreshToken = tokenResponse.required("refresh_token").asText();

        RefreshTokenHistory historyBeforeLogout =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(refreshToken))
                        .orElseThrow();

        assertThat(historyBeforeLogout.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        assertThat(historyBeforeLogout.getRevokedAt()).isNull();

        MvcResult logoutResult =
                mockMvc.perform(
                                get("/connect/logout")
                                        .session(session)
                                        .queryParam("id_token_hint", idToken)
                                        .queryParam(
                                                "post_logout_redirect_uri",
                                                POST_LOGOUT_REDIRECT_URI)
                                        .queryParam("state", LOGOUT_STATE))
                        .andReturn();

        assertThat(logoutResult.getResponse().getStatus())
                .withFailMessage(
                        "OIDC logout failed: status=%s, location=%s, body=%s",
                        logoutResult.getResponse().getStatus(),
                        logoutResult.getResponse().getHeader("Location"),
                        logoutResult.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .isBetween(300, 399);

        assertThat(session.isInvalid()).isTrue();

        RefreshTokenHistory historyAfterLogout =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(refreshToken))
                        .orElseThrow();

        assertThat(historyAfterLogout.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

        assertThat(historyAfterLogout.getRevokedAt()).isNotNull();

        mockMvc.perform(
                        post("/oauth2/token")
                                .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
    }

    private User createActiveUser() {
        return executeInTransaction(
                () -> {
                    String suffix = UUID.randomUUID().toString();

                    String email = "oidc.logout." + suffix + "@example.com";

                    String username = "oidc.logout." + suffix;

                    User user =
                            new User(email, email.toLowerCase(), username, username.toLowerCase());

                    user = userRepository.saveAndFlush(user);

                    userCredentialService.createCredential(user.getId(), RAW_PASSWORD);

                    user.verifyEmail(VERIFIED_AT);

                    user = userRepository.saveAndFlush(user);

                    Role role = roleRepository.findByName(RoleName.USER).orElseThrow();

                    userRoleRepository.saveAndFlush(new UserRole(user, role, VERIFIED_AT, user));

                    return user;
                });
    }

    private RegisteredClient registerConfidentialClient() {
        return executeInTransaction(
                () -> {
                    RegisteredClient existing =
                            registeredClientRepository.findByClientId(CLIENT_ID);

                    if (existing != null) {
                        return existing;
                    }

                    RegisteredClient client =
                            registeredClientFactory.createConfidentialUserClient(
                                    new ConfidentialUserClientRegistration(
                                            CLIENT_ID,
                                            "R25.11.7 OIDC Logout Client",
                                            RAW_CLIENT_SECRET,
                                            Set.of(REDIRECT_URI),
                                            Set.of(POST_LOGOUT_REDIRECT_URI),
                                            Set.of(
                                                    OidcScopes.OPENID,
                                                    "booking:read",
                                                    "booking:create")));

                    registeredClientRepository.save(client);

                    return client;
                });
    }

    private void saveConsent(RegisteredClient client, User user) {

        OAuth2AuthorizationConsent consent =
                OAuth2AuthorizationConsent.withId(client.getId(), user.getUsername())
                        .scope(OidcScopes.OPENID)
                        .scope("booking:read")
                        .build();

        authorizationConsentService.save(consent);
    }

    private MockHttpSession login(User user) throws Exception {

        MvcResult result =
                mockMvc.perform(formLogin().user(user.getUsername()).password(RAW_PASSWORD))
                        .andExpect(authenticated().withUsername(user.getUsername()))
                        .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        SecurityContext securityContext =
                (SecurityContext)
                        session.getAttribute(
                                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        assertThat(securityContext).isNotNull();

        assertThat(securityContext.getAuthentication()).isNotNull();

        assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();

        assertThat(securityContext.getAuthentication().getName()).isEqualTo(user.getUsername());

        return session;
    }

    private String authorize(MockHttpSession session, String codeChallenge) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                get("/oauth2/authorize")
                                        .session(session)
                                        .queryParam("response_type", "code")
                                        .queryParam("client_id", CLIENT_ID)
                                        .queryParam("redirect_uri", REDIRECT_URI)
                                        .queryParam("scope", OidcScopes.OPENID + " booking:read")
                                        .queryParam("state", AUTHORIZATION_STATE)
                                        .queryParam("nonce", NONCE)
                                        .queryParam("code_challenge", codeChallenge)
                                        .queryParam("code_challenge_method", "S256"))
                        .andReturn();

        assertThat(result.getResponse().getStatus())
                .withFailMessage(
                        "Authorization failed: status=%s, location=%s, body=%s",
                        result.getResponse().getStatus(),
                        result.getResponse().getHeader("Location"),
                        result.getResponse().getContentAsString())
                .isBetween(300, 399);

        String location = result.getResponse().getRedirectedUrl();

        assertThat(location).isNotNull();

        UriComponents redirect = UriComponentsBuilder.fromUriString(location).build();

        assertThat(redirect.getPath()).isEqualTo("/login/oauth2/code/cinema");

        assertThat(redirect.getQueryParams().getFirst("state")).isEqualTo(AUTHORIZATION_STATE);

        String authorizationCode = redirect.getQueryParams().getFirst("code");

        assertThat(authorizationCode).isNotBlank();

        return authorizationCode;
    }

    private JsonNode exchangeAuthorizationCode(String authorizationCode, String codeVerifier)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("grant_type", "authorization_code")
                                        .param("code", authorizationCode)
                                        .param("redirect_uri", REDIRECT_URI)
                                        .param("code_verifier", codeVerifier))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.access_token").isNotEmpty())
                        .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                        .andExpect(jsonPath("$.id_token").isNotEmpty())
                        .andExpect(jsonPath("$.token_type").value("Bearer"))
                        .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String createCodeChallenge(String codeVerifier) {

        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private <T> T executeInTransaction(Supplier<T> operation) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return Objects.requireNonNull(transactionTemplate.execute(status -> operation.get()));
    }
}
