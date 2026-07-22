package com.cinema.common.tracing.service;

import java.util.Objects;
import java.util.function.Supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

public class DefaultTracingService
        implements TracingService {

    private final Tracer tracer;

    public DefaultTracingService(
            Tracer tracer) {

        this.tracer = Objects.requireNonNull(
                tracer,
                "Tracer must not be null");

    }

    @Override
    public void runInSpan(
            String spanName,
            Runnable operation) {

        Objects.requireNonNull(
                operation,
                "Operation must not be null");

        trace(
                spanName,
                () -> {

                    operation.run();

                    return null;

                });

    }

    @Override
    public <T> T trace(
            String spanName,
            Supplier<T> operation) {

        validateSpanName(
                spanName);

        Objects.requireNonNull(
                operation,
                "Operation must not be null");

        Span span = tracer
                .nextSpan()
                .name(spanName)
                .start();

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {

            return operation.get();

        } catch (RuntimeException exception) {

            span.error(
                    exception);

            throw exception;

        } catch (Error error) {

            span.error(
                    error);

            throw error;

        } finally {

            span.end();

        }

    }

    @Override
    public void addTag(
            String key,
            String value) {

        if (key == null
                || key.isBlank()
                || value == null) {

            return;

        }

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {

            return;

        }

        currentSpan.tag(
                key,
                value);

    }

    @Override
    public void addEvent(
            String eventName) {

        if (eventName == null
                || eventName.isBlank()) {

            return;

        }

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {

            return;

        }

        currentSpan.event(
                eventName);

    }

    @Override
    public void recordError(
            Throwable throwable) {

        if (throwable == null) {

            return;

        }

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {

            return;

        }

        currentSpan.error(
                throwable);

    }

    private void validateSpanName(
            String spanName) {

        if (spanName == null
                || spanName.isBlank()) {

            throw new IllegalArgumentException(
                    "Span name must not be blank");

        }

    }

}
