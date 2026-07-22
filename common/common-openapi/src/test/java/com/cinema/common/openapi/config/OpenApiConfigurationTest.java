package com.cinema.common.openapi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.cinema.common.openapi.properties.OpenApiProperties;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;

class OpenApiConfigurationTest {

    private final OpenApiConfiguration configuration = new OpenApiConfiguration();

    @Test
    void shouldCreateOpenApiMetadata() {

        OpenApiProperties properties = new OpenApiProperties();

        properties.setTitle(
                "Movie Service API");

        properties.setDescription(
                "Movie service documentation");

        properties.setVersion(
                "1.2.0");

        OpenAPI openApi = configuration.cinemaOpenApi(
                properties);

        assertNotNull(
                openApi);

        assertEquals(
                "Movie Service API",
                openApi.getInfo().getTitle());

        assertEquals(
                "Movie service documentation",
                openApi.getInfo().getDescription());

        assertEquals(
                "1.2.0",
                openApi.getInfo().getVersion());

    }

    @Test
    void shouldConfigureBearerSecurityScheme() {

        OpenAPI openApi = configuration.cinemaOpenApi(
                new OpenApiProperties());

        SecurityScheme securityScheme = openApi.getComponents()
                .getSecuritySchemes()
                .get(
                        OpenApiConfiguration.BEARER_AUTH);

        assertNotNull(
                securityScheme);

        assertEquals(
                SecurityScheme.Type.HTTP,
                securityScheme.getType());

        assertEquals(
                "bearer",
                securityScheme.getScheme());

        assertEquals(
                "JWT",
                securityScheme.getBearerFormat());

        assertTrue(
                openApi.getSecurity()
                        .getFirst()
                        .containsKey(
                                OpenApiConfiguration.BEARER_AUTH));

    }

    @Test
    void shouldAddConfiguredServer() {

        OpenApiProperties properties = new OpenApiProperties();

        properties.setServerUrl(
                "http://localhost:8080");

        properties.setServerDescription(
                "Local server");

        OpenAPI openApi = configuration.cinemaOpenApi(
                properties);

        assertEquals(
                1,
                openApi.getServers().size());

        assertEquals(
                "http://localhost:8080",
                openApi.getServers()
                        .getFirst()
                        .getUrl());

        assertEquals(
                "Local server",
                openApi.getServers()
                        .getFirst()
                        .getDescription());

    }

    @Test
    void shouldNotAddServerWhenUrlIsMissing() {

        OpenAPI openApi = configuration.cinemaOpenApi(
                new OpenApiProperties());

        assertTrue(
                openApi.getServers() == null
                        || openApi.getServers().isEmpty());

    }

}
