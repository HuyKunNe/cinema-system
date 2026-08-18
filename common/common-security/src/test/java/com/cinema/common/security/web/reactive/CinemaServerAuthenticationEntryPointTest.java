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
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;

class CinemaServerAuthenticationEntryPointTest {

    private CinemaServerAuthenticationEntryPoint authenticationEntryPoint;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

        ReactiveSecurityResponseWriter responseWriter =
                new ReactiveSecurityResponseWriter(objectMapper);

        authenticationEntryPoint = new CinemaServerAuthenticationEntryPoint(responseWriter);
    }

    @Test
    void shouldWriteSafeUnauthorizedResponse() {

        MockServerWebExchange exchange = exchange();

        authenticationEntryPoint
                .commence(exchange, new BadCredentialsException("raw bearer token: secret-token"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);

        String body = responseBody(exchange);

        assertThat(body)
                .contains("\"success\":false")
                .contains("\"code\":" + "\"SECURITY_AUTHENTICATION_REQUIRED\"")
                .contains("\"message\":" + "\"Authentication is required\"")
                .contains("\"category\":\"SECURITY\"")
                .doesNotContain("secret-token")
                .doesNotContain("raw bearer token")
                .doesNotContain("BadCredentialsException")
                .doesNotContain("stackTrace");
    }

    @Test
    void constructorShouldRejectMissingResponseWriter() {

        assertThatNullPointerException()
                .isThrownBy(() -> new CinemaServerAuthenticationEntryPoint(null))
                .withMessage("responseWriter must not be null");
    }

    private static MockServerWebExchange exchange() {

        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/bookings").build());
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
