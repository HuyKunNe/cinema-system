package com.cinema.common.security.web.reactive;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;

import com.cinema.common.security.error.SecurityErrorCode;

import reactor.core.publisher.Mono;

public final class CinemaServerAuthenticationEntryPoint
        implements ServerAuthenticationEntryPoint {

    private final ReactiveSecurityResponseWriter responseWriter;

    public CinemaServerAuthenticationEntryPoint(
            ReactiveSecurityResponseWriter responseWriter) {

        this.responseWriter = Objects.requireNonNull(
                responseWriter,
                "responseWriter must not be null");
    }

    @Override
    public Mono<Void> commence(
            ServerWebExchange exchange,
            AuthenticationException exception) {

        return responseWriter.write(
                exchange,
                HttpStatus.UNAUTHORIZED,
                SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }
}
