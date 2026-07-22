package com.cinema.common.tracing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

class DefaultTracingServiceTest {

    private Tracer tracer;

    private Span span;

    private Tracer.SpanInScope spanInScope;

    private DefaultTracingService tracingService;

    @BeforeEach
    void setUp() {

        tracer = mock(
                Tracer.class);

        span = mock(
                Span.class);

        spanInScope = mock(
                Tracer.SpanInScope.class);

        when(
                tracer.nextSpan())
                .thenReturn(
                        span);

        when(
                span.name(
                        "booking.reserve"))
                .thenReturn(
                        span);

        when(
                span.start())
                .thenReturn(
                        span);

        when(
                tracer.withSpan(
                        span))
                .thenReturn(
                        spanInScope);

        tracingService = new DefaultTracingService(
                tracer);

    }

    @Test
    void shouldExecuteOperationInsideSpan() {

        String result = tracingService.trace(
                "booking.reserve",
                () -> "reserved");

        assertEquals(
                "reserved",
                result);

        verify(
                span)
                .start();

        verify(
                tracer)
                .withSpan(
                        span);

        verify(
                spanInScope)
                .close();

        verify(
                span)
                .end();

    }

    @Test
    void shouldRecordExceptionAndRethrow() {

        IllegalStateException exception = new IllegalStateException(
                "Seat unavailable");

        IllegalStateException result = assertThrows(
                IllegalStateException.class,
                () -> tracingService.trace(
                        "booking.reserve",
                        () -> {

                            throw exception;

                        }));

        assertEquals(
                exception,
                result);

        verify(
                span)
                .error(
                        exception);

        verify(
                span)
                .end();

    }

    @Test
    void shouldAddTagToCurrentSpan() {

        when(
                tracer.currentSpan())
                .thenReturn(
                        span);

        tracingService.addTag(
                "booking.id",
                "booking-123");

        verify(
                span)
                .tag(
                        "booking.id",
                        "booking-123");

    }

    @Test
    void shouldAddEventToCurrentSpan() {

        when(
                tracer.currentSpan())
                .thenReturn(
                        span);

        tracingService.addEvent(
                "seat.reserved");

        verify(
                span)
                .event(
                        "seat.reserved");

    }

    @Test
    void shouldIgnoreTagWhenNoSpanExists() {

        when(
                tracer.currentSpan())
                .thenReturn(
                        null);

        tracingService.addTag(
                "booking.id",
                "booking-123");

        verify(
                span,
                never())
                .tag(
                        "booking.id",
                        "booking-123");

    }

    @Test
    void shouldRejectBlankSpanName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> tracingService.trace(
                        " ",
                        () -> "result"));

    }

}
