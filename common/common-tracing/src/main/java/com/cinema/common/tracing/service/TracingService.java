package com.cinema.common.tracing.service;

import java.util.function.Supplier;

public interface TracingService {

    void runInSpan(
            String spanName,
            Runnable operation);

    <T> T trace(
            String spanName,
            Supplier<T> operation);

    void addTag(
            String key,
            String value);

    void addEvent(
            String eventName);

    void recordError(
            Throwable throwable);

}
