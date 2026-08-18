package com.cinema.common.security.web.reactive;

import com.cinema.common.security.error.SecurityErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.Objects;

public final class CinemaServerAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ReactiveSecurityResponseWriter responseWriter;

    public CinemaServerAccessDeniedHandler(ReactiveSecurityResponseWriter responseWriter) {

        this.responseWriter =
                Objects.requireNonNull(responseWriter, "responseWriter must not be null");
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException exception) {

        return responseWriter.write(
                exchange, HttpStatus.FORBIDDEN, SecurityErrorCode.ACCESS_DENIED);
    }
}
