package com.cinema.common.security.web;

import java.io.IOException;

import com.cinema.common.security.error.SecurityErrorCode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class CinemaAccessDeniedHandler
        implements AccessDeniedHandler {

    private final SecurityResponseWriter responseWriter;

    public CinemaAccessDeniedHandler(
            SecurityResponseWriter responseWriter) {

        this.responseWriter = responseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        responseWriter.write(
                response,
                HttpStatus.FORBIDDEN,
                SecurityErrorCode.ACCESS_DENIED);
    }
}
