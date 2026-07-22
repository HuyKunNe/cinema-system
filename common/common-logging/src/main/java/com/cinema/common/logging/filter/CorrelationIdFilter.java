package com.cinema.common.logging.filter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cinema.common.logging.constant.LoggingConstants;
import com.cinema.common.logging.context.LogContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = Optional.ofNullable(
                request.getHeader("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());

        LogContext.put(
                LoggingConstants.CORRELATION_ID,
                correlationId);

        response.setHeader(
                "X-Correlation-Id",
                correlationId);

        try {
            filterChain.doFilter(
                    request,
                    response);
        } finally {
            LogContext.clear();
        }

    }

}
