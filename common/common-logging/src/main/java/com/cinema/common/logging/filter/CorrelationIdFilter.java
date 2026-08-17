package com.cinema.common.logging.filter;

import com.cinema.common.logging.constant.LoggingConstants;
import com.cinema.common.logging.context.LogContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern CORRELATION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,99}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId =
                resolveCorrelationId(request.getHeader(LoggingConstants.CORRELATION_ID_HEADER));

        LogContext.put(LoggingConstants.CORRELATION_ID, correlationId);

        response.setHeader(LoggingConstants.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            LogContext.remove(LoggingConstants.CORRELATION_ID);
        }
    }

    private static String resolveCorrelationId(String suppliedCorrelationId) {

        if (suppliedCorrelationId != null) {
            String normalized = suppliedCorrelationId.trim();

            if (normalized.length() <= LoggingConstants.CORRELATION_ID_MAX_LENGTH
                    && CORRELATION_ID_PATTERN.matcher(normalized).matches()) {

                return normalized;
            }
        }

        return UUID.randomUUID().toString();
    }
}
