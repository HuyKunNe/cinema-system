package com.cinema.common.tracing.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

class MicrometerTraceContextProviderTest {

    private final Tracer tracer = mock(Tracer.class);

    private final MicrometerTraceContextProvider provider = new MicrometerTraceContextProvider(
            tracer);

    @Test
    void shouldReturnCurrentTraceContext() {

        Span span = mock(Span.class);

        TraceContext traceContext = mock(TraceContext.class);

        when(
                tracer.currentSpan())
                .thenReturn(
                        span);

        when(
                span.context())
                .thenReturn(
                        traceContext);

        when(
                traceContext.traceId())
                .thenReturn(
                        "trace-123");

        when(
                traceContext.spanId())
                .thenReturn(
                        "span-456");

        TraceContextInfo result = provider.getCurrentContext();

        assertEquals(
                "trace-123",
                result.traceId());

        assertEquals(
                "span-456",
                result.spanId());

        assertTrue(
                result.isPresent());

    }

    @Test
    void shouldReturnEmptyContextWhenCurrentSpanIsMissing() {

        when(
                tracer.currentSpan())
                .thenReturn(
                        null);

        TraceContextInfo result = provider.getCurrentContext();

        assertFalse(
                result.isPresent());

        assertTrue(
                provider.getCurrentTraceId()
                        .isEmpty());

        assertTrue(
                provider.getCurrentSpanId()
                        .isEmpty());

    }

    @Test
    void shouldReturnEmptyContextWhenSpanContextIsMissing() {

        Span span = mock(Span.class);

        when(
                tracer.currentSpan())
                .thenReturn(
                        span);

        when(
                span.context())
                .thenReturn(
                        null);

        TraceContextInfo result = provider.getCurrentContext();

        assertFalse(
                result.isPresent());

    }

}
