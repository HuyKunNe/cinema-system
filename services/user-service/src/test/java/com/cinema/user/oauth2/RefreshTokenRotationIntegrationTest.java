package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;
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
import com.cinema.user.entity.SecurityAuditEvent;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.oauth2.model.ConfidentialUserClientRegistration;
import com.cinema.user.oauth2.token.RefreshTokenHasher;
import com.cinema.user.oauth2.token.RefreshTokenStatus;
import com.cinema.user.repository.RefreshTokenHistoryRepository;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.SecurityAuditEventRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditTargetType;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@SpringBootTest(
        properties = {
            "spring.main.web-application-type=servlet",
            "cinema.user.authorization-server.issuer=http://localhost:8082",
            "cinema.user.authorization-server.jwt.audiences=cinema-api"
        })
@AutoConfigureMockMvc
class RefreshTokenRotationIntegrationTest extends AbstractMySqlIntegrationTest {

    private static final String ISSUER = "http://localhost:8082";

    private static final String CLIENT_ID = "r25-11-3-cinema-bff";

    private static final String RAW_CLIENT_SECRET = "r25-11-3-confidential-client-secret";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/login/oauth2/code/cinema";

    private static final String POST_LOGOUT_REDIRECT_URI = "http://127.0.0.1:8080";

    private static final String RAW_PASSWORD = "correct-password-123";

    private static final String KEY_ID = "cinema-refresh-rotation-test-key";

    private static final String CODE_VERIFIER =
            "cinema-refresh-token-pkce-verifier-123456789012345678901";

    private static final OffsetDateTime VERIFIED_AT = OffsetDateTime.parse("2026-08-12T03:00:00Z");

    private static final TestRsaKeyMaterial KEY_MATERIAL = TestRsaKeyMaterial.generate();

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Autowired private UserCredentialService userCredentialService;

    @Autowired private RoleRepository roleRepository;

    @Autowired private UserRoleRepository userRoleRepository;

    @Autowired private RegisteredClientFactory registeredClientFactory;

    @Autowired private RegisteredClientRepository registeredClientRepository;

    @Autowired private OAuth2AuthorizationService authorizationService;

    @Autowired private RefreshTokenHistoryRepository refreshTokenHistoryRepository;

    @Autowired private RefreshTokenHasher refreshTokenHasher;

    @Autowired private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired private JwtDecoder jwtDecoder;

    @Autowired private ObjectMapper objectMapper;

    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private SecurityAuditEventRepository securityAuditEventRepository;

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
    void shouldRotateRefreshTokenForConfidentialUserClient() throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String codeChallenge = createCodeChallenge(CODE_VERIFIER);

        assertThat(CODE_VERIFIER.length()).isBetween(43, 128);

        assertThat(codeChallenge).hasSize(43);

        String authorizationCode = authorize(session, codeChallenge);

        JsonNode initialResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String firstAccessToken = initialResponse.required("access_token").asText();

        String firstRefreshToken = initialResponse.required("refresh_token").asText();

        Jwt firstJwt = jwtDecoder.decode(firstAccessToken);

        assertUserToken(firstJwt, user);

        OAuth2Authorization initialAuthorization =
                authorizationService.findByToken(firstRefreshToken, OAuth2TokenType.REFRESH_TOKEN);

        assertThat(initialAuthorization).isNotNull();

        String firstRefreshTokenHash = refreshTokenHasher.hash(firstRefreshToken);

        RefreshTokenHistory initialHistory =
                refreshTokenHistoryRepository.findByTokenHash(firstRefreshTokenHash).orElseThrow();

        assertThat(initialHistory.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        assertThat(initialHistory.getAuthorizationId()).isEqualTo(initialAuthorization.getId());

        assertThat(initialHistory.getRegisteredClientId()).isEqualTo(client.getId());

        assertThat(initialHistory.getPrincipalName()).isEqualTo(user.getUsername());

        assertThat(initialHistory.getTokenHash()).isEqualTo(firstRefreshTokenHash);

        assertThat(initialHistory.getTokenHash()).doesNotContain(firstRefreshToken);

        assertThat(initialHistory.getRotatedAt()).isNull();

        assertThat(initialHistory.getRevokedAt()).isNull();

        assertThat(initialHistory.getReusedAt()).isNull();

        JsonNode refreshedResponse = refreshAccessToken(firstRefreshToken);

        String secondAccessToken = refreshedResponse.required("access_token").asText();

        String secondRefreshToken = refreshedResponse.required("refresh_token").asText();

        Jwt secondJwt = jwtDecoder.decode(secondAccessToken);

        assertUserToken(secondJwt, user);

        assertThat(secondJwt.getId()).isNotEqualTo(firstJwt.getId());

        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        RefreshTokenHistory rotatedHistory =
                refreshTokenHistoryRepository.findByTokenHash(firstRefreshTokenHash).orElseThrow();

        assertThat(rotatedHistory.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);

        assertThat(rotatedHistory.getRotatedAt()).isNotNull();

        assertThat(rotatedHistory.getRevokedAt()).isNull();

        assertThat(rotatedHistory.getReusedAt()).isNull();

        String secondRefreshTokenHash = refreshTokenHasher.hash(secondRefreshToken);

        RefreshTokenHistory activeHistory =
                refreshTokenHistoryRepository.findByTokenHash(secondRefreshTokenHash).orElseThrow();

        assertThat(activeHistory.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        assertThat(activeHistory.getAuthorizationId())
                .isEqualTo(rotatedHistory.getAuthorizationId());

        assertThat(activeHistory.getRegisteredClientId())
                .isEqualTo(rotatedHistory.getRegisteredClientId());

        assertThat(activeHistory.getPrincipalName()).isEqualTo(rotatedHistory.getPrincipalName());

        assertThat(activeHistory.getIssuedAt()).isNotNull();

        assertThat(activeHistory.getExpiresAt()).isAfter(activeHistory.getIssuedAt());

        assertThat(activeHistory.getRotatedAt()).isNull();

        assertThat(activeHistory.getRevokedAt()).isNull();

        assertThat(activeHistory.getReusedAt()).isNull();

        assertThat(
                        refreshTokenHistoryRepository.findAllByAuthorizationId(
                                activeHistory.getAuthorizationId()))
                .hasSize(2)
                .extracting(RefreshTokenHistory::getStatus)
                .containsExactlyInAnyOrder(RefreshTokenStatus.ROTATED, RefreshTokenStatus.ACTIVE);

        OAuth2Authorization currentAuthorization =
                authorizationService.findByToken(secondRefreshToken, OAuth2TokenType.REFRESH_TOKEN);

        assertThat(currentAuthorization).isNotNull();

        assertThat(currentAuthorization.getId()).isEqualTo(initialAuthorization.getId());

        assertThat(
                        authorizationService.findByToken(
                                firstRefreshToken, OAuth2TokenType.REFRESH_TOKEN))
                .isNull();
    }

    @Test
    void concurrentRefreshShouldAllowOnlyOneSuccessfulResponse() throws Exception {

        ConcurrentRefreshFixture fixture = createConcurrentRefreshFixture();

        List<MvcResult> results = performConcurrentRefresh(fixture.refreshToken());

        assertThat(results).hasSize(2);

        List<MvcResult> successfulResults =
                results.stream().filter(result -> result.getResponse().getStatus() == 200).toList();

        List<MvcResult> rejectedResults =
                results.stream().filter(result -> result.getResponse().getStatus() == 400).toList();

        assertThat(successfulResults).hasSize(1);

        assertThat(rejectedResults).hasSize(1);

        JsonNode successfulResponse =
                objectMapper.readTree(
                        successfulResults.getFirst().getResponse().getContentAsString());

        String successorRefreshToken = successfulResponse.required("refresh_token").asText();

        assertThat(successorRefreshToken).isNotBlank().isNotEqualTo(fixture.refreshToken());

        assertThat(rejectedResults.getFirst().getResponse().getContentAsString())
                .contains("\"error\":\"invalid_grant\"")
                .doesNotContain(fixture.refreshToken(), successorRefreshToken);

        List<RefreshTokenHistory> history =
                refreshTokenHistoryRepository.findAllByAuthorizationId(fixture.authorizationId());

        assertThat(history).hasSize(2);

        assertThat(history)
                .filteredOn(token -> token.getStatus() == RefreshTokenStatus.ACTIVE)
                .hasSizeLessThanOrEqualTo(1);

        assertThat(history)
                .filteredOn(
                        token ->
                                token.getTokenHash()
                                        .equals(refreshTokenHasher.hash(successorRefreshToken)))
                .singleElement();

        assertThat(history)
                .allSatisfy(
                        token ->
                                assertThat(token.getTokenHash())
                                        .doesNotContain(
                                                fixture.refreshToken(), successorRefreshToken));
    }

    @Test
    void concurrentRotatedTokenReuseShouldRevokeFamilyOnce() throws Exception {

        ConcurrentRefreshFixture fixture = createConcurrentRefreshFixture();

        JsonNode rotatedResponse = refreshAccessToken(fixture.refreshToken());

        String successorRefreshToken = rotatedResponse.required("refresh_token").asText();

        assertThat(successorRefreshToken).isNotEqualTo(fixture.refreshToken());

        RefreshTokenHistory rotatedBeforeReuse =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(fixture.refreshToken()))
                        .orElseThrow();

        RefreshTokenHistory activeBeforeReuse =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(successorRefreshToken))
                        .orElseThrow();

        assertThat(rotatedBeforeReuse.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);

        assertThat(activeBeforeReuse.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);

        List<MvcResult> results = performConcurrentRefresh(fixture.refreshToken());

        assertThat(results)
                .hasSize(2)
                .allSatisfy(
                        result -> {
                            assertThat(result.getResponse().getStatus()).isEqualTo(400);

                            assertThat(result.getResponse().getContentAsString())
                                    .contains("\"error\":\"invalid_grant\"")
                                    .doesNotContain(fixture.refreshToken(), successorRefreshToken);
                        });

        RefreshTokenHistory reusedHistory =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(fixture.refreshToken()))
                        .orElseThrow();

        RefreshTokenHistory revokedHistory =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(successorRefreshToken))
                        .orElseThrow();

        assertThat(reusedHistory.getStatus()).isEqualTo(RefreshTokenStatus.REUSED);

        assertThat(reusedHistory.getReusedAt()).isNotNull();

        assertThat(revokedHistory.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

        assertThat(revokedHistory.getRevokedAt()).isNotNull();

        assertThat(
                        refreshTokenHistoryRepository.findAllByAuthorizationId(
                                fixture.authorizationId()))
                .hasSize(2)
                .extracting(RefreshTokenHistory::getStatus)
                .containsExactlyInAnyOrder(RefreshTokenStatus.REUSED, RefreshTokenStatus.REVOKED);

        OAuth2Authorization authorization =
                authorizationService.findById(fixture.authorizationId());

        assertThat(authorization).isNotNull();

        assertThat(authorization.getAccessToken()).isNotNull();

        assertThat(authorization.getAccessToken().isInvalidated()).isTrue();

        assertThat(authorization.getRefreshToken()).isNotNull();

        assertThat(authorization.getRefreshToken().isInvalidated()).isTrue();

        List<SecurityAuditEvent> auditEvents =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.AUTHORIZATION_SESSION,
                                fixture.authorizationId());

        assertThat(auditEvents)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType())
                                    .isEqualTo(SecurityAuditEventType.REFRESH_TOKEN_REUSE_DETECTED);

                            assertThat(event.getActorType())
                                    .isEqualTo(SecurityAuditActorType.CLIENT);

                            assertThat(event.getActorReference()).isEqualTo(CLIENT_ID);

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.AUTHORIZATION_SESSION);

                            assertThat(event.getTargetReference())
                                    .isEqualTo(fixture.authorizationId());

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getReason()).isEqualTo("ROTATED_REFRESH_TOKEN_REUSED");

                            assertThat(event.getMetadata()).isNull();

                            assertThat(event.getCorrelationId()).isNotBlank();

                            assertThat(event.getOccurredAt()).isNotNull();
                        });
    }

    @Test
    void shouldRejectRotatedRefreshTokenReuse() throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String authorizationCode = authorize(session, createCodeChallenge(CODE_VERIFIER));

        JsonNode initialResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String firstRefreshToken = initialResponse.required("refresh_token").asText();

        JsonNode rotatedResponse = refreshAccessToken(firstRefreshToken);

        String secondRefreshToken = rotatedResponse.required("refresh_token").asText();

        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);

        mockMvc.perform(
                        post("/oauth2/token")
                                .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", firstRefreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));

        RefreshTokenHistory reusedHistory =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(firstRefreshToken))
                        .orElseThrow();

        assertThat(reusedHistory.getStatus()).isEqualTo(RefreshTokenStatus.REUSED);

        assertThat(reusedHistory.getReusedAt()).isNotNull();

        RefreshTokenHistory revokedHistory =
                refreshTokenHistoryRepository
                        .findByTokenHash(refreshTokenHasher.hash(secondRefreshToken))
                        .orElseThrow();

        assertThat(revokedHistory.getStatus()).isEqualTo(RefreshTokenStatus.REVOKED);

        assertThat(revokedHistory.getRevokedAt()).isNotNull();

        mockMvc.perform(
                        post("/oauth2/token")
                                .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", secondRefreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));

        OAuth2Authorization authorization =
                authorizationService.findById(reusedHistory.getAuthorizationId());

        assertThat(authorization).isNotNull();

        assertThat(authorization.getRefreshToken().isInvalidated()).isTrue();

        assertThat(authorization.getAccessToken().isInvalidated()).isTrue();

        List<SecurityAuditEvent> auditEvents =
                securityAuditEventRepository
                        .findAllByTargetTypeAndTargetReferenceOrderByOccurredAtDesc(
                                SecurityAuditTargetType.AUTHORIZATION_SESSION,
                                reusedHistory.getAuthorizationId());

        assertThat(auditEvents)
                .singleElement()
                .satisfies(
                        event -> {
                            assertThat(event.getEventType())
                                    .isEqualTo(SecurityAuditEventType.REFRESH_TOKEN_REUSE_DETECTED);

                            assertThat(event.getActorType())
                                    .isEqualTo(SecurityAuditActorType.CLIENT);

                            assertThat(event.getActorReference()).isEqualTo(CLIENT_ID);

                            assertThat(event.getTargetType())
                                    .isEqualTo(SecurityAuditTargetType.AUTHORIZATION_SESSION);

                            assertThat(event.getTargetReference())
                                    .isEqualTo(reusedHistory.getAuthorizationId());

                            assertThat(event.getOutcome()).isEqualTo(SecurityAuditOutcome.SUCCESS);

                            assertThat(event.getReason()).isEqualTo("ROTATED_REFRESH_TOKEN_REUSED");

                            assertThat(event.getMetadata()).isNull();

                            assertThat(event.getOccurredAt()).isNotNull();

                            assertThat(event.getCreatedAt()).isNotNull();

                            assertThat(event.getUpdatedAt()).isNotNull();
                        });
    }

    @Test
    void shouldRejectRefreshRequestWithoutClientAuthentication() throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String authorizationCode = authorize(session, createCodeChallenge(CODE_VERIFIER));

        JsonNode initialResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String refreshToken = initialResponse.required("refresh_token").asText();

        mockMvc.perform(
                        post("/oauth2/token")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", refreshToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    @Test
    void shouldRejectIncorrectClientSecret() throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String authorizationCode = authorize(session, createCodeChallenge(CODE_VERIFIER));

        JsonNode initialResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String refreshToken = initialResponse.required("refresh_token").asText();

        mockMvc.perform(
                        post("/oauth2/token")
                                .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET + "-incorrect"))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_client"));
    }

    private User createActiveUser() {
        return executeInTransaction(
                () -> {
                    String suffix = UUID.randomUUID().toString();

                    User user =
                            new User(
                                    "refresh." + suffix + "@example.com",
                                    "refresh." + suffix + "@example.com",
                                    "refresh." + suffix,
                                    "refresh." + suffix);

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
                                            "R25.11.3 Cinema BFF",
                                            RAW_CLIENT_SECRET,
                                            Set.of(REDIRECT_URI),
                                            Set.of(POST_LOGOUT_REDIRECT_URI),
                                            Set.of("booking:read", "booking:create")));

                    registeredClientRepository.save(client);

                    return client;
                });
    }

    private void saveConsent(RegisteredClient client, User user) {

        OAuth2AuthorizationConsent consent =
                OAuth2AuthorizationConsent.withId(client.getId(), user.getUsername())
                        .scope("booking:read")
                        .build();

        authorizationConsentService.save(consent);
    }

    private ConcurrentRefreshFixture createConcurrentRefreshFixture() throws Exception {

        User user = createActiveUser();

        RegisteredClient client = registerConfidentialClient();

        saveConsent(client, user);

        MockHttpSession session = login(user);

        String authorizationCode = authorize(session, createCodeChallenge(CODE_VERIFIER));

        JsonNode initialResponse = exchangeAuthorizationCode(authorizationCode, CODE_VERIFIER);

        String refreshToken = initialResponse.required("refresh_token").asText();

        OAuth2Authorization authorization =
                authorizationService.findByToken(refreshToken, OAuth2TokenType.REFRESH_TOKEN);

        assertThat(authorization).isNotNull();

        return new ConcurrentRefreshFixture(refreshToken, authorization.getId());
    }

    private List<MvcResult> performConcurrentRefresh(String refreshToken) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);

        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> first =
                    executor.submit(
                            () -> {
                                ready.countDown();

                                if (!start.await(10, TimeUnit.SECONDS)) {

                                    throw new IllegalStateException(
                                            "Concurrent refresh start timed out");
                                }

                                return performRefreshRequest(refreshToken);
                            });

            Future<MvcResult> second =
                    executor.submit(
                            () -> {
                                ready.countDown();

                                if (!start.await(10, TimeUnit.SECONDS)) {

                                    throw new IllegalStateException(
                                            "Concurrent refresh start timed out");
                                }

                                return performRefreshRequest(refreshToken);
                            });

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        } finally {
            start.countDown();
            executor.shutdownNow();

            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private MvcResult performRefreshRequest(String refreshToken) throws Exception {

        return mockMvc.perform(
                        post("/oauth2/token")
                                .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .param("grant_type", "refresh_token")
                                .param("refresh_token", refreshToken))
                .andReturn();
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
                                        .queryParam("scope", "booking:read")
                                        .queryParam("state", "refresh-test-state")
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

        assertThat(redirect.getQueryParams().getFirst("state")).isEqualTo("refresh-test-state");

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
                        .andExpect(jsonPath("$.token_type").value("Bearer"))
                        .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode refreshAccessToken(String refreshToken) throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post("/oauth2/token")
                                        .with(httpBasic(CLIENT_ID, RAW_CLIENT_SECRET))
                                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                        .param("grant_type", "refresh_token")
                                        .param("refresh_token", refreshToken))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.access_token").isNotEmpty())
                        .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                        .andExpect(jsonPath("$.token_type").value("Bearer"))
                        .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertUserToken(Jwt jwt, User user) {

        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsEntry("kid", KEY_ID);

        assertThat(jwt.getIssuer()).hasToString(ISSUER);

        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());

        assertThat(UUID.fromString(jwt.getSubject()).version()).isEqualTo(7);

        assertThat(jwt.getAudience()).containsExactly("cinema-api");

        assertThat(jwt.getClaimAsString("username")).isEqualTo(user.getUsername());

        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");

        assertThat(jwt.getClaimAsStringList("permissions")).containsExactly("booking:read");

        assertThat(jwt.getIssuedAt()).isNotNull();

        assertThat(jwt.getExpiresAt()).isNotNull();

        assertThat(jwt.getId()).isNotBlank();
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

    private record ConcurrentRefreshFixture(String refreshToken, String authorizationId) {}
}
