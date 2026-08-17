package com.cinema.common.logging.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.logging.constant.LoggingConstants;
import com.cinema.common.logging.context.LogContext;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearLogContext() {
        LogContext.clear();
    }

    @Test
    void validCorrelationIdShouldBeNormalizedAndPreserved() throws Exception {

        MockHttpServletRequest request = requestWithCorrelationId("  request-123.test:456  ");

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> valueInsideChain = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (currentRequest, currentResponse) ->
                        valueInsideChain.set(LogContext.get(LoggingConstants.CORRELATION_ID)));

        assertThat(valueInsideChain.get()).isEqualTo("request-123.test:456");

        assertThat(response.getHeader(LoggingConstants.CORRELATION_ID_HEADER))
                .isEqualTo("request-123.test:456");

        assertThat(LogContext.get(LoggingConstants.CORRELATION_ID)).isNull();
    }

    @Test
    void maximumLengthCorrelationIdShouldBeAccepted() throws Exception {

        String suppliedCorrelationId = "a".repeat(LoggingConstants.CORRELATION_ID_MAX_LENGTH);

        MockHttpServletRequest request = requestWithCorrelationId(suppliedCorrelationId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (currentRequest, currentResponse) ->
                        assertThat(LogContext.get(LoggingConstants.CORRELATION_ID))
                                .isEqualTo(suppliedCorrelationId));

        assertThat(response.getHeader(LoggingConstants.CORRELATION_ID_HEADER))
                .isEqualTo(suppliedCorrelationId);
    }

    @Test
    void oversizedCorrelationIdShouldBeReplaced() throws Exception {

        String suppliedCorrelationId = "a".repeat(LoggingConstants.CORRELATION_ID_MAX_LENGTH + 1);

        String generated = filterAndReturnCorrelationId(suppliedCorrelationId);

        assertGeneratedUuid(generated);

        assertThat(generated).isNotEqualTo(suppliedCorrelationId);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "   ",
                "request id with spaces",
                "request/id",
                "request?id=1",
                "request#fragment",
                "<script>",
                "request@domain"
            })
    void invalidCorrelationIdShouldBeReplaced(String suppliedCorrelationId) throws Exception {

        String generated = filterAndReturnCorrelationId(suppliedCorrelationId);

        assertGeneratedUuid(generated);

        assertThat(generated).isNotEqualTo(suppliedCorrelationId);
    }

    @Test
    void missingCorrelationIdShouldGenerateUuid() throws Exception {

        String generated = filterAndReturnCorrelationId(null);

        assertGeneratedUuid(generated);
    }

    @Test
    void correlationIdShouldBeRemovedWhenChainFails() {
        MockHttpServletRequest request = requestWithCorrelationId("request-chain-failure");

        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        request,
                                        response,
                                        (currentRequest, currentResponse) -> {
                                            throw new ServletException(
                                                    "simulated downstream failure");
                                        }))
                .isInstanceOf(ServletException.class)
                .hasMessage("simulated downstream failure");

        assertThat(LogContext.get(LoggingConstants.CORRELATION_ID)).isNull();
    }

    private String filterAndReturnCorrelationId(String suppliedCorrelationId)
            throws ServletException, IOException {

        MockHttpServletRequest request = requestWithCorrelationId(suppliedCorrelationId);

        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> valueInsideChain = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (currentRequest, currentResponse) ->
                        valueInsideChain.set(LogContext.get(LoggingConstants.CORRELATION_ID)));

        assertThat(response.getHeader(LoggingConstants.CORRELATION_ID_HEADER))
                .isEqualTo(valueInsideChain.get());

        assertThat(LogContext.get(LoggingConstants.CORRELATION_ID)).isNull();

        return valueInsideChain.get();
    }

    private static MockHttpServletRequest requestWithCorrelationId(String correlationId) {

        MockHttpServletRequest request = new MockHttpServletRequest();

        if (correlationId != null) {
            request.addHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);
        }

        return request;
    }

    private static void assertGeneratedUuid(String value) {

        assertThat(value).isNotBlank().hasSize(36);

        assertThat(UUID.fromString(value)).isNotNull();
    }
}
