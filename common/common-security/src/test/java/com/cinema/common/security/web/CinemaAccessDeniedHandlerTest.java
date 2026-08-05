package com.cinema.common.security.web;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class CinemaAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    private final CinemaAccessDeniedHandler handler = new CinemaAccessDeniedHandler(
            new SecurityResponseWriter(objectMapper));

    @Test
    void shouldWriteStandardForbiddenResponse()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();

        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                request,
                response,
                new AccessDeniedException(
                        "Missing required permission"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType())
                .startsWith("application/json");

        JsonNode body = objectMapper.readTree(
                response.getContentAsByteArray());

        assertThat(body.path("success").asBoolean())
                .isFalse();

        assertThat(body.path("timestamp").asText())
                .isNotBlank();

        assertThat(body.path("error").path("code").asText())
                .isEqualTo("SECURITY_ACCESS_DENIED");

        assertThat(body.path("error").path("message").asText())
                .isEqualTo("Access is denied");

        assertThat(body.path("error").path("category").asText())
                .isEqualTo("SECURITY");
    }
}
