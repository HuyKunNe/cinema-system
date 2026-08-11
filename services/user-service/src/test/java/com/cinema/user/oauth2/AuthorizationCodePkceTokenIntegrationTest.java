package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import com.cinema.user.entity.UserRole;
import com.cinema.user.enums.RoleName;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.repository.RoleRepository;
import com.cinema.user.repository.UserRepository;
import com.cinema.user.repository.UserRoleRepository;
import com.cinema.user.service.UserCredentialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082",
        "cinema.user.authorization-server.jwt.audiences=cinema-api"
})
@AutoConfigureMockMvc
@Import(AuthorizationCodePkceTokenIntegrationTest.ConsentTestConfiguration.class)
class AuthorizationCodePkceTokenIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String ISSUER = "http://localhost:8082";
    private static final String CLIENT_ID = "r25-10-7-cinema-web";
    private static final String REDIRECT_URI = "http://127.0.0.1:3000/callback";
    private static final String POST_LOGOUT_REDIRECT_URI = "http://127.0.0.1:3000";
    private static final String RAW_PASSWORD = "correct-password-123";
    private static final String KEY_ID = "cinema-user-auth-code-test-key";
    private static final String CODE_VERIFIER = "cinema-authorization-code-pkce-verifier-12345678901234567890";
    private static final OffsetDateTime VERIFIED_AT = OffsetDateTime.parse("2026-08-11T03:00:00Z");

    private static final TestRsaKeyMaterial KEY_MATERIAL = TestRsaKeyMaterial.generate();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialService userCredentialService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RegisteredClientFactory registeredClientFactory;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureSigningProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "cinema.user.authorization-server.signing.enabled",
                () -> "true");
        registry.add(
                "cinema.user.authorization-server.signing.key-id",
                () -> KEY_ID);
        registry.add(
                "cinema.user.authorization-server.signing.private-key-location",
                KEY_MATERIAL::privateKeyLocation);
        registry.add(
                "cinema.user.authorization-server.signing.public-key-location",
                KEY_MATERIAL::publicKeyLocation);
    }

    @Test
    void shouldIssueUserJwtThroughAuthorizationCodeWithPkce()
            throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerPublicClient();

        assertPersistedClient(client);
        saveConsent(client, user);

        MockHttpSession session = login(user);
        String codeChallenge = createCodeChallenge(CODE_VERIFIER);

        assertThat(CODE_VERIFIER.length()).isBetween(43, 128);
        assertThat(codeChallenge).hasSize(43);

        String code = authorize(session, codeChallenge);
        JsonNode response = exchangeCode(code, CODE_VERIFIER);

        Jwt jwt = jwtDecoder.decode(
                response.required("access_token").asText());

        assertUserToken(jwt, user);
        assertThat(response.has("refresh_token"))
                .as("Public PKCE client must not receive a refresh token")
                .isFalse();
    }

    @Test
    void shouldRejectIncorrectPkceVerifier()
            throws Exception {

        User user = createActiveUser();
        RegisteredClient client = registerPublicClient();

        assertPersistedClient(client);
        saveConsent(client, user);

        MockHttpSession session = login(user);
        String code = authorize(
                session,
                createCodeChallenge(CODE_VERIFIER));

        mockMvc.perform(
                post("/oauth2/token")
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "grant_type",
                                "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param(
                                "code_verifier",
                                CODE_VERIFIER + "-incorrect"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("invalid_grant"));
    }

    private User createActiveUser() {
        return executeInTransaction(() -> {
            String suffix = UUID.randomUUID().toString();

            User user = new User(
                    "pkce." + suffix + "@example.com",
                    "pkce." + suffix + "@example.com",
                    "pkce." + suffix,
                    "pkce." + suffix);

            user = userRepository.saveAndFlush(user);

            userCredentialService.createCredential(
                    user.getId(),
                    RAW_PASSWORD);

            user.verifyEmail(VERIFIED_AT);
            user = userRepository.saveAndFlush(user);

            Role role = roleRepository
                    .findByName(RoleName.USER)
                    .orElseThrow();

            userRoleRepository.saveAndFlush(
                    new UserRole(
                            user,
                            role,
                            VERIFIED_AT,
                            user));

            return user;
        });
    }

    private RegisteredClient registerPublicClient() {
        return executeInTransaction(() -> {
            RegisteredClient existing = registeredClientRepository.findByClientId(
                    CLIENT_ID);

            if (existing != null) {
                return existing;
            }

            RegisteredClient client = registeredClientFactory.createPublicClient(
                    new PublicClientRegistration(
                            CLIENT_ID,
                            "R25.10.7 Cinema Web",
                            Set.of(REDIRECT_URI),
                            Set.of(POST_LOGOUT_REDIRECT_URI),
                            Set.of(
                                    "booking:read",
                                    "booking:create")));

            registeredClientRepository.save(client);
            return client;
        });
    }

    private void assertPersistedClient(
            RegisteredClient expectedClient) {

        RegisteredClient persistedClient = registeredClientRepository.findByClientId(
                CLIENT_ID);

        assertThat(persistedClient).isNotNull();
        assertThat(persistedClient.getId())
                .isEqualTo(expectedClient.getId());
        assertThat(UUID.fromString(persistedClient.getId()).version())
                .isEqualTo(7);
        assertThat(persistedClient.getRedirectUris())
                .containsExactly(REDIRECT_URI);
        assertThat(persistedClient.getScopes())
                .containsExactlyInAnyOrder(
                        "booking:read",
                        "booking:create");
        assertThat(persistedClient.getAuthorizationGrantTypes())
                .containsOnly(
                        AuthorizationGrantType.AUTHORIZATION_CODE);
        assertThat(persistedClient.getClientAuthenticationMethods())
                .containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(persistedClient.getClientSettings()
                .isRequireProofKey())
                .isTrue();
        assertThat(persistedClient.getClientSettings()
                .isRequireAuthorizationConsent())
                .isTrue();
    }

    private void saveConsent(
            RegisteredClient client,
            User user) {

        OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent
                .withId(
                        client.getId(),
                        user.getUsername())
                .scope("booking:read")
                .build();

        authorizationConsentService.save(consent);
    }

    private MockHttpSession login(User user)
            throws Exception {

        MvcResult result = mockMvc.perform(
                formLogin()
                        .user(user.getUsername())
                        .password(RAW_PASSWORD))
                .andExpect(authenticated()
                        .withUsername(user.getUsername()))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest()
                .getSession(false);

        assertThat(session).isNotNull();

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().isAuthenticated())
                .isTrue();
        assertThat(securityContext.getAuthentication().getName())
                .isEqualTo(user.getUsername());

        return session;
    }

    private String authorize(
            MockHttpSession session,
            String codeChallenge) throws Exception {

        MvcResult result = mockMvc.perform(
                get("/oauth2/authorize")
                        .session(session)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", CLIENT_ID)
                        .queryParam("redirect_uri", REDIRECT_URI)
                        .queryParam("scope", "booking:read")
                        .queryParam("state", "test-state")
                        .queryParam(
                                "code_challenge",
                                codeChallenge)
                        .queryParam(
                                "code_challenge_method",
                                "S256"))
                .andDo(print())
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

        UriComponents redirect = UriComponentsBuilder
                .fromUriString(location)
                .build();

        assertThat(redirect.getPath()).isEqualTo("/callback");
        assertThat(redirect.getQueryParams().getFirst("state"))
                .isEqualTo("test-state");

        String code = redirect.getQueryParams().getFirst("code");
        assertThat(code).isNotBlank();

        return code;
    }

    private JsonNode exchangeCode(
            String code,
            String codeVerifier) throws Exception {

        MvcResult result = mockMvc.perform(
                post("/oauth2/token")
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "grant_type",
                                "authorization_code")
                        .param("client_id", CLIENT_ID)
                        .param("code", code)
                        .param("redirect_uri", REDIRECT_URI)
                        .param("code_verifier", codeVerifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token")
                        .isNotEmpty())
                .andExpect(jsonPath("$.token_type")
                        .value("Bearer"))
                .andReturn();

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8));
    }

    private void assertUserToken(
            Jwt jwt,
            User user) {

        assertThat(jwt.getHeaders())
                .containsEntry("alg", "RS256")
                .containsEntry("kid", KEY_ID);
        assertThat(jwt.getIssuer().toString())
                .isEqualTo(ISSUER);
        assertThat(jwt.getSubject())
                .isEqualTo(user.getId().toString());
        assertThat(UUID.fromString(jwt.getSubject()).version())
                .isEqualTo(7);
        assertThat(jwt.getAudience())
                .containsExactly("cinema-api");
        assertThat(jwt.getClaimAsString("username"))
                .isEqualTo(user.getUsername());
        assertThat(jwt.getClaimAsStringList("roles"))
                .containsExactly("USER");
        assertThat(jwt.getClaimAsStringList("permissions"))
                .containsExactly("booking:read");
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getId()).isNotBlank();
        assertThat(Duration.between(
                jwt.getIssuedAt(),
                jwt.getExpiresAt()))
                .isEqualTo(Duration.ofMinutes(15));
    }

    private String createCodeChallenge(
            String codeVerifier) {

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(
                            StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception);
        }
    }

    private <T> T executeInTransaction(
            Supplier<T> operation) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return Objects.requireNonNull(
                transactionTemplate.execute(
                        status -> operation.get()));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ConsentTestConfiguration {

        @Bean
        OAuth2AuthorizationConsentService authorizationConsentService() {
            return new InMemoryOAuth2AuthorizationConsentService();
        }
    }
}
