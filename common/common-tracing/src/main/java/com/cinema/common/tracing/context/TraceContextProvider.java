package com.cinema.common.tracing.context;

import java.util.Optional;

public interface TraceContextProvider {

    TraceContextInfo getCurrentContext();

    default Optional<String> getCurrentTraceId() {

        return Optional.ofNullable(
                getCurrentContext().traceId());

    }

    default Optional<String> getCurrentSpanId() {

        return Optional.ofNullable(
                getCurrentContext().spanId());

    }

}
