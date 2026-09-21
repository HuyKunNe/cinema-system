package com.cinema.common.openapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cinema.common.openapi.properties.OpenApiProperties;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void shouldCreateOpenApiMetadata() {

        OpenApiProperties properties = new OpenApiProperties();

        properties.setTitle("Movie Service API");

        properties.setDescription("Movie service documentation");

        properties.setVersion("1.2.0");

        OpenAPI openApi = configuration.cinemaOpenApi(properties);

        assertNotNull(openApi);

        assertEquals("Movie Service API", openApi.getInfo().getTitle());

        assertEquals("Movie service documentation", openApi.getInfo().getDescription());

        assertEquals("1.2.0", openApi.getInfo().getVersion());
    }

    @Test
    void shouldConfigureOAuth2AuthorizationCodeSecurityScheme() {

        OpenApiProperties properties = new OpenApiProperties();

        OpenAPI openApi = configuration.cinemaOpenApi(properties);

        SecurityScheme securityScheme =
                openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.OAUTH2);

        assertNotNull(securityScheme);

        assertEquals(SecurityScheme.Type.OAUTH2, securityScheme.getType());

        assertNotNull(securityScheme.getFlows());

        OAuthFlow authorizationCode = securityScheme.getFlows().getAuthorizationCode();

        assertNotNull(authorizationCode);

        assertEquals(
                "http://localhost:8082/oauth2/authorize", authorizationCode.getAuthorizationUrl());

        assertEquals("http://localhost:8082/oauth2/token", authorizationCode.getTokenUrl());

        assertNotNull(authorizationCode.getScopes());

        assertTrue(authorizationCode.getScopes().containsKey("openid"));

        assertTrue(authorizationCode.getScopes().containsKey("profile"));

        assertTrue(authorizationCode.getScopes().containsKey("email"));

        assertTrue(authorizationCode.getScopes().containsKey("movie:manage"));

        assertTrue(authorizationCode.getScopes().containsKey("inventory:manage"));

        assertTrue(authorizationCode.getScopes().containsKey("booking:create"));

        assertTrue(authorizationCode.getScopes().containsKey("booking:read"));

        assertTrue(authorizationCode.getScopes().containsKey("booking:cancel"));

        assertTrue(authorizationCode.getScopes().containsKey("showtime:manage"));

        assertTrue(authorizationCode.getScopes().containsKey("payment:read"));

        assertTrue(authorizationCode.getScopes().containsKey("user:manage"));

        assertTrue(openApi.getSecurity().getFirst().containsKey(OpenApiConfiguration.OAUTH2));
    }

    @Test
    void shouldUseConfiguredOAuth2Endpoints() {

        OpenApiProperties properties = new OpenApiProperties();

        properties.setAuthorizationUrl("https://auth.example.com/oauth2/authorize");

        properties.setTokenUrl("https://auth.example.com/oauth2/token");

        OpenAPI openApi = configuration.cinemaOpenApi(properties);

        SecurityScheme securityScheme =
                openApi.getComponents().getSecuritySchemes().get(OpenApiConfiguration.OAUTH2);

        OAuthFlow authorizationCode = securityScheme.getFlows().getAuthorizationCode();

        assertEquals(
                "https://auth.example.com/oauth2/authorize",
                authorizationCode.getAuthorizationUrl());

        assertEquals("https://auth.example.com/oauth2/token", authorizationCode.getTokenUrl());
    }

    @Test
    void shouldAddConfiguredServer() {

        OpenApiProperties properties = new OpenApiProperties();

        properties.setServerUrl("http://localhost:8080");

        properties.setServerDescription("Local server");

        OpenAPI openApi = configuration.cinemaOpenApi(properties);

        assertEquals(1, openApi.getServers().size());

        assertEquals("http://localhost:8080", openApi.getServers().getFirst().getUrl());

        assertEquals("Local server", openApi.getServers().getFirst().getDescription());
    }

    @Test
    void shouldNotAddServerWhenUrlIsMissing() {

        OpenAPI openApi = configuration.cinemaOpenApi(new OpenApiProperties());

        assertTrue(openApi.getServers() == null || openApi.getServers().isEmpty());
    }
}
