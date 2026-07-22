package com.cinema.common.tracing.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.cinema.common.tracing.context.MicrometerTraceContextProvider;
import com.cinema.common.tracing.context.TraceContextProvider;
import com.cinema.common.tracing.service.DefaultTracingService;
import com.cinema.common.tracing.service.TracingService;

import io.micrometer.tracing.Tracer;

@AutoConfiguration
@ConditionalOnBean(Tracer.class)
public class TracingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TraceContextProvider traceContextProvider(
            Tracer tracer) {

        return new MicrometerTraceContextProvider(
                tracer);

    }

    @Bean
    @ConditionalOnMissingBean
    public TracingService tracingService(
            Tracer tracer) {

        return new DefaultTracingService(
                tracer);

    }

}
