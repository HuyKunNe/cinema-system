package com.cinema.common.security.web.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;

class CinemaServerAccessDeniedHandlerTest {

    private CinemaServerAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

        ReactiveSecurityResponseWriter responseWriter =
                new ReactiveSecurityResponseWriter(objectMapper);

        accessDeniedHandler = new CinemaServerAccessDeniedHandler(responseWriter);
    }

    @Test
    void shouldWriteSafeForbiddenResponse() {

        MockServerWebExchange exchange = exchange();

        accessDeniedHandler
                .handle(exchange, new AccessDeniedException("private authorization detail"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);

        String body = responseBody(exchange);

        assertThat(body)
                .contains("\"success\":false")
                .contains("\"code\":\"SECURITY_ACCESS_DENIED\"")
                .contains("\"message\":\"Access is denied\"")
                .contains("\"category\":\"SECURITY\"")
                .doesNotContain("private authorization detail")
                .doesNotContain("AccessDeniedException")
                .doesNotContain("stackTrace");
    }

    @Test
    void constructorShouldRejectMissingResponseWriter() {

        assertThatNullPointerException()
                .isThrownBy(() -> new CinemaServerAccessDeniedHandler(null))
                .withMessage("responseWriter must not be null");
    }

    private static MockServerWebExchange exchange() {

        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/users").build());
    }

    private static String responseBody(MockServerWebExchange exchange) {

        DataBuffer buffer = DataBufferUtils.join(exchange.getResponse().getBody()).block();

        if (buffer == null) {
            return "";
        }

        try {
            byte[] bytes = new byte[buffer.readableByteCount()];

            buffer.read(bytes);

            return new String(bytes, StandardCharsets.UTF_8);
        } finally {
            DataBufferUtils.release(buffer);
        }
    }
}
