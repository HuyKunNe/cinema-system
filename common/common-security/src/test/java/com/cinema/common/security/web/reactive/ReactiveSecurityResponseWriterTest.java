package com.cinema.common.security.web.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.security.error.SecurityErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.nio.charset.StandardCharsets;

class ReactiveSecurityResponseWriterTest {

    private ReactiveSecurityResponseWriter responseWriter;

    @BeforeEach
    void setUp() {

        ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

        responseWriter = new ReactiveSecurityResponseWriter(objectMapper);
    }

    @Test
    void shouldWriteStandardJsonSecurityResponse() {

        MockServerWebExchange exchange = exchange();

        responseWriter
                .write(exchange, HttpStatus.FORBIDDEN, SecurityErrorCode.ACCESS_DENIED)
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
                .doesNotContain("stackTrace")
                .doesNotContain("exception");
    }

    @Test
    void constructorShouldRejectMissingObjectMapper() {

        assertThatNullPointerException()
                .isThrownBy(() -> new ReactiveSecurityResponseWriter(null))
                .withMessage("objectMapper must not be null");
    }

    @Test
    void writeShouldRejectMissingExchange() {

        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                responseWriter.write(
                                        null,
                                        HttpStatus.UNAUTHORIZED,
                                        SecurityErrorCode.AUTHENTICATION_REQUIRED))
                .withMessage("exchange must not be null");
    }

    @Test
    void writeShouldRejectMissingStatus() {

        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                responseWriter.write(
                                        exchange(), null, SecurityErrorCode.ACCESS_DENIED))
                .withMessage("status must not be null");
    }

    @Test
    void writeShouldRejectMissingErrorCode() {

        assertThatNullPointerException()
                .isThrownBy(() -> responseWriter.write(exchange(), HttpStatus.FORBIDDEN, null))
                .withMessage("errorCode must not be null");
    }

    private static MockServerWebExchange exchange() {

        return MockServerWebExchange.from(MockServerHttpRequest.get("/protected").build());
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
