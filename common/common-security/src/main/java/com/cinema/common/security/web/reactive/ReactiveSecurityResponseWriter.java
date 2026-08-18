package com.cinema.common.security.web.reactive;

import com.cinema.common.exception.code.ErrorCode;
import com.cinema.common.response.factory.ResponseFactory;
import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.Objects;

public final class ReactiveSecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public ReactiveSecurityResponseWriter(ObjectMapper objectMapper) {

        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {

        Objects.requireNonNull(exchange, "exchange must not be null");

        Objects.requireNonNull(status, "status must not be null");

        Objects.requireNonNull(errorCode, "errorCode must not be null");

        ErrorBody error =
                new ErrorBody(
                        errorCode.code(), errorCode.message(), errorCode.category().name(), null);

        ApiResponse<Void> body = ResponseFactory.error(error);

        byte[] payload;

        try {
            payload = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            return Mono.error(exception);
        }

        exchange.getResponse().setStatusCode(status);

        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        exchange.getResponse().getHeaders().setContentLength(payload.length);

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(payload);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
