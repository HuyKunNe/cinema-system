package com.cinema.common.security.web;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class CinemaAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final CinemaAuthenticationEntryPoint entryPoint = new CinemaAuthenticationEntryPoint(
            new SecurityResponseWriter(objectMapper));

    @Test
    void shouldWriteStandardUnauthorizedResponse()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new BadCredentialsException(
                        "Invalid bearer token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType())
                .startsWith("application/json");

        JsonNode body = objectMapper.readTree(
                response.getContentAsByteArray());

        assertThat(body.path("success").asBoolean())
                .isFalse();

        assertThat(body.path("timestamp").asText())
                .isNotBlank();

        assertThat(body.path("error").path("code").asText())
                .isEqualTo(
                        "SECURITY_AUTHENTICATION_REQUIRED");

        assertThat(body.path("error").path("message").asText())
                .isEqualTo("Authentication is required");

        assertThat(body.path("error").path("category").asText())
                .isEqualTo("SECURITY");
    }
}
