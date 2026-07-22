package com.cinema.common.tracing.context;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;

public class MicrometerTraceContextProvider
        implements TraceContextProvider {

    private final Tracer tracer;

    public MicrometerTraceContextProvider(
            Tracer tracer) {

        this.tracer = tracer;

    }

    @Override
    public TraceContextInfo getCurrentContext() {

        Span currentSpan = tracer.currentSpan();

        if (currentSpan == null) {

            return TraceContextInfo.empty();

        }

        TraceContext context = currentSpan.context();

        if (context == null) {

            return TraceContextInfo.empty();

        }

        return new TraceContextInfo(
                context.traceId(),
                context.spanId());

    }

}
