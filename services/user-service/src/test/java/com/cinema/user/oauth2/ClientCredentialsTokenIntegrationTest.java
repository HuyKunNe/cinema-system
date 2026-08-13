package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082",
        "cinema.user.authorization-server.jwt.audiences=cinema-api"
})
@AutoConfigureMockMvc
class ClientCredentialsTokenIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String ISSUER = "http://localhost:8082";

    private static final String REGISTERED_CLIENT_ID = "019c5000-0000-7000-8000-000000000106";

    private static final String CLIENT_ID = "r25-10-6-inventory-service";

    private static final String RAW_CLIENT_SECRET = "local-client-credentials-secret";

    private static final String ROTATED_CLIENT_SECRET = "rotated-client-credentials-secret";

    private static final String KEY_ID = "cinema-user-token-integration-key";

    private static final TestRsaKeyMaterial KEY_MATERIAL = TestRsaKeyMaterial.generate();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private OAuth2ClientLifecycleService clientLifecycleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private ObjectMapper objectMapper;

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

    @BeforeEach
    void registerServiceClient() {
        if (registeredClientRepository.findByClientId(
                CLIENT_ID) != null) {

            return;
        }

        assertThat(UUID.fromString(
                REGISTERED_CLIENT_ID).version())
                .isEqualTo(7);

        RegisteredClient client = serviceClient(
                REGISTERED_CLIENT_ID,
                CLIENT_ID,
                RAW_CLIENT_SECRET,
                "R25.10.6 Inventory Service");

        registeredClientRepository.save(
                client);
    }

    @Test
    void shouldIssueSignedJwtForClientCredentials()
            throws Exception {

        MvcResult result = requestToken(
                CLIENT_ID,
                RAW_CLIENT_SECRET,
                "inventory:read inventory:write")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").isNumber())
                .andExpect(jsonPath("$.scope").isNotEmpty())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8));

        String tokenValue = response.required(
                "access_token")
                .asText();

        Jwt jwt = jwtDecoder.decode(
                tokenValue);

        assertServiceToken(
                jwt,
                CLIENT_ID);
    }

    @Test
    void shouldRejectIncorrectClientSecret()
            throws Exception {

        requestToken(
                CLIENT_ID,
                "incorrect-secret",
                "inventory:read")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value(
                                "invalid_client"));
    }

    @Test
    void shouldRejectUnregisteredScope()
            throws Exception {

        requestToken(
                CLIENT_ID,
                RAW_CLIENT_SECRET,
                "user:admin")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value(
                                "invalid_scope"));
    }

    @Test
    void deactivatedClientShouldBeRejectedAsInvalidClient()
            throws Exception {

        ClientFixture fixture = registerIsolatedServiceClient(
                "deactivate");

        requestToken(
                fixture.clientId(),
                fixture.rawSecret(),
                "inventory:read")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token")
                        .isNotEmpty());

        clientLifecycleService.deactivate(
                fixture.clientId());

        requestToken(
                fixture.clientId(),
                fixture.rawSecret(),
                "inventory:read")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value(
                                "invalid_client"));

        assertThat(registeredClientRepository.findByClientId(
                fixture.clientId()))
                .isNull();
    }

    @Test
    void rotatedSecretShouldRejectOldSecretAndAcceptNewSecret()
            throws Exception {

        ClientFixture fixture = registerIsolatedServiceClient(
                "rotate");

        requestToken(
                fixture.clientId(),
                fixture.rawSecret(),
                "inventory:read")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token")
                        .isNotEmpty());

        clientLifecycleService.rotateSecret(
                fixture.clientId(),
                ROTATED_CLIENT_SECRET);

        requestToken(
                fixture.clientId(),
                fixture.rawSecret(),
                "inventory:read")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value(
                                "invalid_client"));

        MvcResult result = requestToken(
                fixture.clientId(),
                ROTATED_CLIENT_SECRET,
                "inventory:read inventory:write")
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token")
                        .isNotEmpty())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse()
                        .getContentAsString(
                                StandardCharsets.UTF_8));

        Jwt jwt = jwtDecoder.decode(
                response.required(
                        "access_token")
                        .asText());

        assertServiceToken(
                jwt,
                fixture.clientId());
    }

    private org.springframework.test.web.servlet.ResultActions requestToken(
            String clientId,
            String rawSecret,
            String scopes)
            throws Exception {

        return mockMvc.perform(
                post("/oauth2/token")
                        .with(httpBasic(
                                clientId,
                                rawSecret))
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED)
                        .param(
                                "grant_type",
                                "client_credentials")
                        .param(
                                "scope",
                                scopes));
    }

    private ClientFixture registerIsolatedServiceClient(
            String prefix) {

        String suffix = UUID.randomUUID()
                .toString();

        String registeredClientId = UUID.randomUUID()
                .toString();

        String clientId = "r25-11-8-5-"
                + prefix
                + "-"
                + suffix;

        String rawSecret = "isolated-"
                + prefix
                + "-secret";

        RegisteredClient client = serviceClient(
                registeredClientId,
                clientId,
                rawSecret,
                "R25.11.8.5 "
                        + prefix
                        + " Client");

        registeredClientRepository.save(
                client);

        return new ClientFixture(
                clientId,
                rawSecret);
    }

    private RegisteredClient serviceClient(
            String registeredClientId,
            String clientId,
            String rawSecret,
            String clientName) {

        return RegisteredClient
                .withId(
                        registeredClientId)
                .clientId(
                        clientId)
                .clientIdIssuedAt(
                        Instant.now())
                .clientSecret(
                        passwordEncoder.encode(
                                rawSecret))
                .clientName(
                        clientName)
                .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(
                        "inventory:read")
                .scope(
                        "inventory:write")
                .clientSettings(
                        ClientSettings.builder()
                                .requireAuthorizationConsent(
                                        false)
                                .build())
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenFormat(
                                        OAuth2TokenFormat.SELF_CONTAINED)
                                .accessTokenTimeToLive(
                                        Duration.ofMinutes(5))
                                .build())
                .build();
    }

    private void assertServiceToken(
            Jwt jwt,
            String expectedSubject) {

        assertThat(jwt.getHeaders())
                .containsEntry(
                        "alg",
                        "RS256")
                .containsEntry(
                        "kid",
                        KEY_ID);

        assertThat(jwt.getIssuer().toString())
                .isEqualTo(
                        ISSUER);

        assertThat(jwt.getSubject())
                .isEqualTo(
                        expectedSubject);

        assertThat(jwt.getAudience())
                .containsExactly(
                        "cinema-api");

        assertThat(jwt.getClaimAsStringList(
                "permissions"))
                .containsExactly(
                        "inventory:read",
                        "inventory:write");

        assertThat(jwt.getClaims())
                .doesNotContainKeys(
                        "username",
                        "roles");

        assertThat(jwt.getIssuedAt())
                .isNotNull();

        assertThat(jwt.getExpiresAt())
                .isNotNull();

        assertThat(jwt.getId())
                .isNotBlank();

        assertThat(Duration.between(
                jwt.getIssuedAt(),
                jwt.getExpiresAt()))
                .isEqualTo(
                        Duration.ofMinutes(5));
    }

    private record ClientFixture(
            String clientId,
            String rawSecret) {
    }
}
