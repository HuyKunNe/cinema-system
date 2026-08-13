package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082"
})
@AutoConfigureMockMvc
class AuthorizationServerSecurityConfigurationTest
        extends AbstractMySqlIntegrationTest {

    private static final String ISSUER = "http://localhost:8082";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private List<SecurityFilterChain> securityFilterChains;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void shouldRegisterSeparateSecurityFilterChains() {
        assertThat(securityFilterChains)
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void authorizationEndpointShouldBeHandledByAuthorizationServerChain()
            throws Exception {

        mockMvc.perform(get("/oauth2/authorize"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginPageShouldBeAvailable()
            throws Exception {

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointShouldRemainPublic()
            throws Exception {

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedApplicationEndpointShouldRedirectToLogin()
            throws Exception {

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        endsWith("/login")));
    }

    @Test
    void authorizationServerMetadataShouldExposeCanonicalIssuer()
            throws Exception {

        mockMvc.perform(get(
                "/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer")
                        .value(ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint")
                        .value(
                                ISSUER
                                        + "/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint")
                        .value(
                                ISSUER
                                        + "/oauth2/token"));
    }

    @Test
    void tokenEndpointShouldReturnOAuthErrorForInvalidRequest()
            throws Exception {

        mockMvc.perform(post("/oauth2/token")
                .param(
                        "grant_type",
                        "authorization_code"))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(
                                "application/json"))
                .andExpect(jsonPath("$.error")
                        .exists());
    }

    @Test
    void oidcProviderConfigurationShouldExposeLogoutEndpoint()
            throws Exception {

        mockMvc.perform(get(
                "/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer")
                        .value(
                                ISSUER))
                .andExpect(jsonPath("$.authorization_endpoint")
                        .value(
                                ISSUER
                                        + "/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint")
                        .value(
                                ISSUER
                                        + "/oauth2/token"))
                .andExpect(jsonPath("$.end_session_endpoint")
                        .value(
                                ISSUER
                                        + "/connect/logout"));
    }

    @Test
    void shouldUseJpaUserDetailsService() {
        assertThat(userDetailsService)
                .isInstanceOf(
                        com.cinema.user.security.JpaUserDetailsService.class);
    }
}
