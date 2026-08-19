package com.cinema.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
public final class UntrustedIdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final List<String> UNTRUSTED_IDENTITY_HEADERS =
            List.of("X-User-Id", "X-Roles", "X-Permissions", "X-Client-Id");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest sanitizedRequest =
                exchange.getRequest()
                        .mutate()
                        .headers(headers -> UNTRUSTED_IDENTITY_HEADERS.forEach(headers::remove))
                        .build();

        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        return chain.filter(sanitizedExchange);
    }

    @Override
    public int getOrder() {

        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
