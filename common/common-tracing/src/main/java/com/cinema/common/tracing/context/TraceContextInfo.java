package com.cinema.common.tracing.context;

public record TraceContextInfo(
        String traceId,
        String spanId) {

    public static TraceContextInfo empty() {

        return new TraceContextInfo(
                null,
                null);

    }

    public boolean isPresent() {

        return traceId != null
                && !traceId.isBlank();

    }

}
