package com.cinema.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RequestIdGlobalFilterTest {

    private final RequestIdGlobalFilter filter = new RequestIdGlobalFilter();

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() {

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/v1/movies")
                        .build());

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        GatewayFilterChain chain = filteredExchange -> {
            capturedExchange.set(
                    filteredExchange);

            return Mono.empty();
        };

        StepVerifier.create(
                filter.filter(
                        exchange,
                        chain))
                .verifyComplete();

        String requestId = capturedExchange.get()
                .getRequest()
                .getHeaders()
                .getFirst(
                        RequestIdGlobalFilter.REQUEST_ID_HEADER);

        assertThat(requestId)
                .isNotBlank();

        assertThat(
                exchange.getResponse()
                        .getHeaders()
                        .getFirst(
                                RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(requestId);
    }

    @Test
    void shouldPreserveExistingRequestId() {

        String existingRequestId = "request-123";

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest
                        .get("/api/v1/bookings")
                        .header(
                                RequestIdGlobalFilter.REQUEST_ID_HEADER,
                                existingRequestId)
                        .build());

        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        GatewayFilterChain chain = filteredExchange -> {
            capturedExchange.set(
                    filteredExchange);

            return Mono.empty();
        };

        StepVerifier.create(
                filter.filter(
                        exchange,
                        chain))
                .verifyComplete();

        assertThat(
                capturedExchange.get()
                        .getRequest()
                        .getHeaders()
                        .getFirst(
                                RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(existingRequestId);

        assertThat(
                exchange.getResponse()
                        .getHeaders()
                        .getFirst(
                                RequestIdGlobalFilter.REQUEST_ID_HEADER))
                .isEqualTo(existingRequestId);
    }
}
