package com.cinema.user.oauth2;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.cinema.user.oauth2.model.PublicClientRegistration;
import com.cinema.user.oauth2.model.ServiceClientRegistration;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082"
})
@AutoConfigureMockMvc
@Transactional
class OAuth2ProtocolPolicyIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String REDIRECT_URI = "http://127.0.0.1:3000/callback";

    private static final String CODE_CHALLENGE = "bUJ6xqg2lMgH-s1pPyg09ykKDquC9hYQXLa3kT6UUXI";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2ClientRegistrationService registrationService;

    private String publicClientId;

    private String serviceClientId;

    @BeforeEach
    void registerClients() {
        String suffix = UUID.randomUUID().toString();

        publicClientId = "protocol-web-" + suffix;
        serviceClientId = "protocol-service-" + suffix;

        registrationService.registerPublicClient(
                new PublicClientRegistration(
                        publicClientId,
                        "Protocol Web",
                        Set.of(REDIRECT_URI),
                        Set.of(),
                        Set.of(
                                "openid",
                                "booking:read")));

        registrationService.registerServiceClient(
                new ServiceClientRegistration(
                        serviceClientId,
                        "Protocol Service",
                        "protocol-service-secret",
                        Set.of("inventory:write")));
    }

    @Test
    void metadataShouldAdvertiseApprovedGrantTypes()
            throws Exception {

        mockMvc.perform(get(
                "/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.grant_types_supported[*]",
                        containsInAnyOrder(
                                "authorization_code",
                                "client_credentials",
                                "refresh_token")))
                .andExpect(jsonPath(
                        "$.code_challenge_methods_supported[*]",
                        containsInAnyOrder("S256")));
    }

    @Test
    void publicAuthorizationShouldRejectMissingPkceChallenge()
            throws Exception {

        mockMvc.perform(get("/oauth2/authorize")
                .with(user("protocol-user"))
                .queryParam("response_type", "code")
                .queryParam("client_id", publicClientId)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("scope", "booking:read")
                .queryParam("state", "missing-pkce"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern(
                        REDIRECT_URI
                                + "?error=invalid_request*code_challenge*"));
    }

    @Test
    void publicAuthorizationWithS256PkceShouldReachConsent()
            throws Exception {

        mockMvc.perform(get("/oauth2/authorize")
                .with(user("protocol-user"))
                .queryParam("response_type", "code")
                .queryParam("client_id", publicClientId)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("scope", "booking:read")
                .queryParam("state", "valid-pkce")
                .queryParam("code_challenge", CODE_CHALLENGE)
                .queryParam("code_challenge_method", "S256"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "text/html"))
                .andExpect(content().string(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString(
                                        "Consent required"),
                                org.hamcrest.Matchers.containsString(
                                        publicClientId),
                                org.hamcrest.Matchers.containsString(
                                        "booking:read"))));
    }

    @Test
    void serviceClientShouldNotUseAuthorizationCodeEndpoint()
            throws Exception {

        mockMvc.perform(get("/oauth2/authorize")
                .with(user("protocol-user"))
                .queryParam("response_type", "code")
                .queryParam("client_id", serviceClientId)
                .queryParam("redirect_uri", REDIRECT_URI)
                .queryParam("scope", "inventory:write")
                .queryParam("state", "service-code"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientCredentialsShouldRejectIncorrectSecret()
            throws Exception {

        mockMvc.perform(post("/oauth2/token")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic(
                                serviceClientId,
                                "incorrect-secret"))
                .param("grant_type", "client_credentials")
                .param("scope", "inventory:write"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("invalid_client"));
    }

    @Test
    void publicClientShouldNotAuthenticateWithClientSecret()
            throws Exception {

        mockMvc.perform(post("/oauth2/token")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic(
                                publicClientId,
                                "invented-secret"))
                .param("grant_type", "authorization_code")
                .param("code", "not-a-code")
                .param("redirect_uri", REDIRECT_URI)
                .param("code_verifier", "not-a-verifier"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("invalid_client"));
    }
}
